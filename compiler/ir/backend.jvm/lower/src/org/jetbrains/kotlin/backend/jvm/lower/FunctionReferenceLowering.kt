/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ir.moveBodyTo
import org.jetbrains.kotlin.backend.common.lower.SamEqualsHashCodeMethodsGenerator
import org.jetbrains.kotlin.backend.common.lower.UpgradeCallableReferences
import org.jetbrains.kotlin.backend.common.lower.VariableRemapper
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.declarationsAtFunctionReferenceLowering
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.JvmSymbols
import org.jetbrains.kotlin.backend.jvm.ir.*
import org.jetbrains.kotlin.backend.jvm.lower.indy.*
import org.jetbrains.kotlin.config.JvmClosureGenerationScheme
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrStarProjection
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_SERIALIZABLE_LAMBDA_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import kotlin.collections.plus

/**
 * Constructs instances of anonymous KFunction subclasses for function references.
 */
internal class FunctionReferenceLowering(private val context: JvmBackendContext) : FileLoweringPass, IrElementTransformerVoidWithContext() {
    private val crossinlineLambdas = HashSet<IrSimpleFunction>()

    private val IrRichFunctionReference.isInlineLambda: Boolean
        get() = origin == IrStatementOrigin.INLINE_LAMBDA

    private val IrRichFunctionReference.isSuspendLambda: Boolean
        get() = invokeFunction.isSuspend && origin.isLambda && type.isSuspendFunctionOrKFunction()

    private val IrRichFunctionReference.isIgnored: Boolean
        get() = isSuspendLambda || isInlineLambda

    // `suspend` function references are the same as non-`suspend` ones, just with an extra continuation parameter;
    // however, suspending lambdas require different generation implemented in SuspendLambdaLowering
    // because they are also their own continuation classes.
    // TODO: Currently, origin of callable references explicitly written in source code is null. Do we need to create one?
    private fun IrRichFunctionReference.isSuspendFunctionReference(): Boolean = overriddenFunctionSymbol.isSuspend &&
            (origin == null || origin == IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE || origin == IrStatementOrigin.SUSPEND_CONVERSION)

    override fun lower(irFile: IrFile) {
        irFile.findRichInlineLambdas(context) { argument, _, parameter, _ ->
            if (parameter.isCrossinline) {
                crossinlineLambdas.add(argument.invokeFunction)
            }
        }
        withinScope(irFile) {
            irFile.transformChildrenVoid(this)
        }
        crossinlineLambdas.clear()
    }

    private val shouldGenerateIndySamConversions =
        context.config.samConversionsScheme == JvmClosureGenerationScheme.INDY

    private val shouldGenerateIndyLambdas: Boolean
        get() = context.config.lambdasScheme == JvmClosureGenerationScheme.INDY
                // We prefer CLASS lambdas when evaluating expression in debugger, as such lambdas have pretty toString implementation
                // However, it's safe to change compilation scheme only for lambdas defined in code fragment, not it's dependencies
                && allScopes.none { (it.irElement as? IrMetadataSourceOwner)?.metadata is MetadataSource.CodeFragment }

    private val shouldGenerateLightweightLambdas: Boolean
        get() = shouldGenerateIndyLambdas && context.config.languageVersionSettings.supportsFeature(LanguageFeature.LightweightLambdas)

    private val isJavaSamConversionWithEqualsHashCode =
        context.config.languageVersionSettings.supportsFeature(LanguageFeature.JavaSamConversionEqualsHashCode)

    override fun visitRichFunctionReference(expression: IrRichFunctionReference): IrExpression {
        return processReference(expression, forceSerializability = false) // todo forceSerializability
    }

    override fun visitClassNew(declaration: IrClass): IrStatement {
        if (declaration.isFun || declaration.symbol.isSuspendFunction() || declaration.symbol.isKSuspendFunction()) {
            declaration.declarationsAtFunctionReferenceLowering = declaration.declarations.toList()
        }
        declaration.transformChildrenVoid()
        return declaration
    }

    private fun processReference(expression: IrRichFunctionReference, forceSerializability: Boolean): IrExpression {
        expression.transformChildrenVoid(this)
        if (expression.isIgnored) return expression
        if (shouldGenerateIndyLambdas && expression.origin.isLambda && !expression.isSamConversion()) {
            val lambdaMetafactoryArguments =
                LambdaMetafactoryArgumentsBuilder(context, crossinlineLambdas)
                    .getLambdaMetafactoryArguments(expression, expression.type, plainLambda = true, forceSerializability = forceSerializability)
            if (lambdaMetafactoryArguments is LambdaMetafactoryArguments) {
                expression.indyCallData = IndyCallData(lambdaMetafactoryArguments.shouldBeSerializable)
                return expression
            }
            // TODO MetafactoryArgumentsResult.Failure.FunctionHazard?
        }


        if (shouldGenerateIndySamConversions && expression.isSamConversion()) {
            val lambdaMetafactoryArguments =
                LambdaMetafactoryArgumentsBuilder(context, crossinlineLambdas)
                    .getLambdaMetafactoryArguments(expression, expression.type, plainLambda = false, forceSerializability = false)
            if (lambdaMetafactoryArguments is LambdaMetafactoryArguments) {
                return expression.apply {
                    indyCallData = IndyCallData(lambdaMetafactoryArguments.shouldBeSerializable)
                    type = expression.type
                }
            }
        }
        return FunctionRichReferenceBuilder(expression, expression.type.takeIf { expression.isSamConversion() }).build()
    }

    private fun IrRichFunctionReference.isSamConversion(): Boolean =
        !type.isFunctionOrKFunction() && !type.isSuspendFunctionOrKFunction()

    private fun getDeclarationParentForDelegatingLambda(): IrDeclarationParent {
        for (s in allScopes.asReversed()) {
            val scopeOwner = s.scope.scopeOwnerSymbol.owner
            if (scopeOwner is IrDeclarationParent) {
                return scopeOwner
            }
        }
        throw AssertionError(
            "No IrDeclarationParent found in scopes:\n" +
                    allScopes.joinToString(separator = "\n") { "  " + it.scope.scopeOwnerSymbol.owner.render() }
        )
    }

    // Handle SAM conversions which wrap a function reference:
    //     class sam$n(private val receiver: R) : Interface { override fun method(...) = receiver.target(...) }
    //
    // This avoids materializing an invokable KFunction representing, thus producing one less class.
    // This is actually very common, as `Interface { something }` is a local function + a SAM-conversion
    // of a reference to it into an implementation.
    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
        if (expression.operator != IrTypeOperator.SAM_CONVERSION) {
            return super.visitTypeOperator(expression)
        }

        val samSuperType = expression.typeOperand

        val invokable = expression.argument

        if (shouldGenerateIndySamConversions && !(invokable is IrRichFunctionReference && invokable.origin.isLambda) && canGenerateIndySamConversionOnFunctionalExpression(samSuperType, invokable)) {
            val lambdaBlock = SamDelegatingLambdaBuilder(context)
                .build(invokable, samSuperType, currentScope!!.scope.scopeOwnerSymbol, getDeclarationParentForDelegatingLambda())
            val lambdaMetafactoryArguments = LambdaMetafactoryArgumentsBuilder(context, crossinlineLambdas)
                .getLambdaMetafactoryArguments(lambdaBlock.ref, samSuperType, plainLambda = false, forceSerializability = false)

            if (lambdaMetafactoryArguments !is LambdaMetafactoryArguments) {
                // TODO MetafactoryArgumentsResult.Failure.FunctionHazard?
                return super.visitTypeOperator(expression)
            }

            // This is what IR contains if SAM-converted argument needs additional cast
            // See ArgumentsGenerationUtilsKt.castArgumentToFunctionalInterfaceForSamType (K1)
            // or AdapterGenerator.castArgumentToFunctionalInterfaceForSamType (K2)
            // In this case we should propagate the serialization flag from the SAM type regardless of the delegating lambda type (see KT-70306)
            if (invokable is IrTypeOperatorCall && invokable.operator == IrTypeOperator.IMPLICIT_CAST && invokable.argument is IrRichFunctionReference) {
                invokable.argument = processReference(invokable.argument as IrRichFunctionReference, lambdaMetafactoryArguments.shouldBeSerializable)
            } else {
                invokable.transformChildrenVoid()
            }

            return wrapSamDelegatingLambdaWithIndySamConversion(samSuperType, lambdaBlock, lambdaMetafactoryArguments)
        }

        if (invokable !is IrRichFunctionReference) return super.visitTypeOperator(expression)

        invokable.transformChildrenVoid(this)

        if (shouldGenerateIndySamConversions) {
            val lambdaMetafactoryArguments =
                LambdaMetafactoryArgumentsBuilder(context, crossinlineLambdas)
                    .getLambdaMetafactoryArguments(invokable, samSuperType, plainLambda = false, forceSerializability = false)
            if (lambdaMetafactoryArguments is LambdaMetafactoryArguments) {
                return invokable.apply {
                    indyCallData = IndyCallData(lambdaMetafactoryArguments.shouldBeSerializable)
                    overriddenFunctionSymbol = lambdaMetafactoryArguments.samMethod.symbol
                    type = samSuperType
                }
            }
        }

        // Erase generic arguments in the SAM type, because they are not easy to approximate correctly otherwise,
        // and LambdaMetafactory also uses erased type.
        val erasedSamSuperType = samSuperType.erasedUpperBound.rawType(context)

        return FunctionRichReferenceBuilder(invokable, erasedSamSuperType).build()
    }

    private fun createProxyLocalFunctionForIndySamConversion(reference: IrRichFunctionReference): IrBlock {
        val startOffset = reference.startOffset
        val endOffset = reference.endOffset
        val targetFun = reference.invokeFunction

        // For a function reference with possibly bound value parameters
        //      [ dispatchReceiver = ..., extensionReceiver = ..., ... ]::foo
        // create a proxy wrapper:
        //      {
        //          val tmp_proxy_0 = <bound_argument_value_0>
        //          ...
        //          val tmp_proxy_N = <bound_argument_value_N>
        //          fun `$proxy`(p_0: TP_0, ..., p_M: TP_M): TR =
        //              foo(... arg_J ...)
        //              // here for each J arg_J is either 'tmp_proxy_K' or 'p_K' for some K
        //          ::`$proxy`
        //      }

        val temporaryVals = ArrayList<IrVariable>()

        val targetCall: IrFunctionAccessExpression = IrCallImpl.fromSymbolOwner(startOffset, endOffset, targetFun.symbol)

        val proxyFun = context.irFactory.buildFun {
            name = Name.identifier("${targetFun.name.asString()}__proxy")
            returnType = targetFun.returnType.eraseTypeParameters()
            visibility = DescriptorVisibilities.LOCAL
            modality = Modality.FINAL
            isSuspend = false
            isInline = false
            origin =
                if (targetFun.isInline || targetFun.isArrayOf())
                    JvmLoweredDeclarationOrigin.PROXY_FUN_FOR_METAFACTORY
                else
                    JvmLoweredDeclarationOrigin.SYNTHETIC_PROXY_FUN_FOR_METAFACTORY
        }.also { proxyFun ->
            proxyFun.parent = currentDeclarationParent
                ?: throw AssertionError("No declaration parent when processing $reference")

            var temporaryValIndex = 0
            var proxyParameterIndex = 0

            fun addAndGetTemporaryVal(initializer: IrExpression): IrGetValue {
                val tmpVal = IrVariableImpl(
                    startOffset, endOffset,
                    IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
                    IrVariableSymbolImpl(),
                    Name.identifier("tmp_proxy_${temporaryValIndex++}"),
                    initializer.type,
                    isVar = false, isConst = false, isLateinit = false
                )
                tmpVal.initializer = initializer
                tmpVal.parent = proxyFun.parent
                temporaryVals.add(tmpVal)
                return IrGetValueImpl(startOffset, endOffset, tmpVal.symbol)
            }

            fun addAndGetProxyValueParameter(originalParameter: IrValueParameter): IrGetValue {
                val proxyParameter = buildValueParameter(proxyFun) {
                    updateFrom(originalParameter)
                    name = Name.identifier("p$proxyParameterIndex\$${originalParameter.name.asString()}")
                    type = originalParameter.type.eraseTypeParameters()
                    proxyParameterIndex++
                    kind = IrParameterKind.Regular
                }.apply {
                    parent = proxyFun
                }
                proxyFun.parameters += proxyParameter
                return IrGetValueImpl(startOffset, endOffset, proxyParameter.symbol)
            }

            fun getTargetCallArgument(boundValue: IrExpression?, originalParameter: IrValueParameter?): IrExpression? =
                when {
                    boundValue != null ->
                        addAndGetTemporaryVal(boundValue)
                    originalParameter != null ->
                        addAndGetProxyValueParameter(originalParameter)
                    else ->
                        null
                }

            targetCall.arguments.assignFrom(reference.boundValues zip targetFun.parameters) { (boundValue, parameter) ->
                getTargetCallArgument(boundValue, parameter)
            }

            val proxyFunBody = context.irFactory.createBlockBody(startOffset, endOffset).also { proxyFun.body = it }
            when {
                targetFun.returnType.isUnit() -> {
                    proxyFunBody.statements.add(targetCall)
                }
                else -> {
                    proxyFunBody.statements.add(
                        IrReturnImpl(
                            startOffset, endOffset,
                            context.irBuiltIns.nothingType,
                            proxyFun.symbol,
                            targetCall
                        )
                    )
                }
            }
        }

        val proxyFunRef = IrRichFunctionReferenceImpl(
            startOffset = startOffset,
            endOffset = endOffset,
            type = reference.type,
            invokeFunction = proxyFun,
            reflectionTargetSymbol = reference.reflectionTargetSymbol,
            origin = reference.origin,
            overriddenFunctionSymbol = reference.overriddenFunctionSymbol,
            hasUnitConversion = reference.hasUnitConversion,
            hasSuspendConversion = reference.hasSuspendConversion,
            hasVarargConversion = reference.hasVarargConversion,
            isRestrictedSuspension = reference.isRestrictedSuspension,
        )

        return IrBlockImpl(
            startOffset, endOffset,
            reference.type,
            origin = null,
            temporaryVals + proxyFun + proxyFunRef
        )
    }

    private fun canGenerateIndySamConversionOnFunctionalExpression(samSuperType: IrType, expression: IrExpression): Boolean {
        val samClass = samSuperType.classOrNull
            ?: throw AssertionError("Class type expected: ${samSuperType.render()}")
        if (!samClass.owner.isFromJava() || isJavaSamConversionWithEqualsHashCode)
            return false
        if (expression is IrRichFunctionReference && expression.origin == IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE)
            return false
        return true
    }

    private fun wrapSamDelegatingLambdaWithIndySamConversion(
        samSuperType: IrType,
        lambdaBlock: SamDelegatingLambdaBlock,
        lambdaMetafactoryArguments: LambdaMetafactoryArguments,
    ): IrExpression {
        val indySamConversion = wrapWithIndySamConversion(samSuperType, lambdaMetafactoryArguments, origin = lambdaBlock.ref.origin)
        lambdaBlock.replaceRefWith(indySamConversion)
        return lambdaBlock.block
    }

    private fun wrapSamConversionArgumentWithIndySamConversion(
        expression: IrTypeOperatorCall,
        produceSamConversion: (IrType) -> IrExpression,
    ): IrExpression {
        val samType = expression.typeOperand
        if (expression.argument !is IrRichFunctionReference) {
            throw AssertionError("Function reference expected: ${expression.render()}")
        }
        return produceSamConversion(samType)
    }

    private val specialNullabilityAnnotationsFqNames =
        setOf(
            JvmSymbols.FLEXIBLE_NULLABILITY_ANNOTATION_FQ_NAME,
            JvmAnnotationNames.ENHANCED_NULLABILITY_ANNOTATION,
        )

    private fun wrapWithIndySamConversion(
        samType: IrType,
        lambdaMetafactoryArguments: LambdaMetafactoryArguments,
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET,
        origin: IrStatementOrigin? = null,
    ): IrExpression {
        val notNullSamType = samType.makeNotNull()
            .removeAnnotations { it.type.classFqName in specialNullabilityAnnotationsFqNames }
        val parent = currentScope!!.scope.getLocalDeclarationParent()
        return context.createJvmIrBuilder(currentScope!!, startOffset, endOffset).run {
            // See [org.jetbrains.kotlin.backend.jvm.JvmSymbols::indyLambdaMetafactoryIntrinsic].
            val implMethodReference = lambdaMetafactoryArguments.implMethodReference.let(::replaceWithStaticCallOrThis)
            val calculatedInvokeFunction = implMethodReference.symbol.owner
            fun IrFunction.isAdaptable() =
                when (this.origin) {
                    IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA,
                    JvmLoweredDeclarationOrigin.PROXY_FUN_FOR_METAFACTORY,
                    JvmLoweredDeclarationOrigin.SYNTHETIC_PROXY_FUN_FOR_METAFACTORY,
                        -> true
                    IrDeclarationOrigin.LOCAL_FUNCTION -> isAnonymousFunction
                    else -> false
                }

            val invokeFunction = if (false && calculatedInvokeFunction.isAdaptable()) {
                calculatedInvokeFunction as IrSimpleFunction
            } else if (false && calculatedInvokeFunction.isFakeOverride) {
                calculatedInvokeFunction as IrSimpleFunction
            } else {
                with(UpgradeCallableReferences(this@FunctionReferenceLowering.context).UpgradeTransformer()) {
                    val arguments = implMethodReference.getCapturedValues()
                    implMethodReference.wrapFunction(
                        captured = arguments,
                        parent = parent,
                        referencedFunction = calculatedInvokeFunction,
                    )
                }
            }
            IrRichFunctionReferenceImpl(
                startOffset = UNDEFINED_OFFSET, endOffset = UNDEFINED_OFFSET,
                type = notNullSamType,
                overriddenFunctionSymbol = lambdaMetafactoryArguments.samMethod.symbol,
                invokeFunction = invokeFunction,
                reflectionTargetSymbol = null,
                origin = origin,
            ).apply {
                indyCallData = IndyCallData(lambdaMetafactoryArguments.shouldBeSerializable)
                boundValues.addAll(implMethodReference.arguments.filterNotNull())
            }
//            irCall(jvmIndyLambdaMetafactoryIntrinsic, notNullSamType).apply {
//                typeArguments[0] = notNullSamType
//                arguments[0] = irRawFunctionRef(lambdaMetafactoryArguments.samMethod)
//                arguments[1] = lambdaMetafactoryArguments.implMethodReference
//                arguments[2] = irRawFunctionRef(lambdaMetafactoryArguments.fakeInstanceMethod)
//                arguments[3] = irVarargOfRawFunctionRefs(lambdaMetafactoryArguments.extraOverriddenMethods)
//                arguments[4] = irBoolean(lambdaMetafactoryArguments.shouldBeSerializable)
//            }
        }
    }

    private fun replaceWithStaticCallOrThis(implFunRef: IrFunctionReference): IrFunctionReference {
        val implFun = implFunRef.symbol.owner
        if (implFunRef.dispatchReceiver != null && implFun is IrSimpleFunction && implFun.isJvmStaticDeclaration()) {
            val (staticProxy, _) = context.cachedDeclarations.getStaticAndCompanionDeclaration(implFun)
            return IrFunctionReferenceImpl(
                implFunRef.startOffset, implFunRef.endOffset, implFunRef.type,
                staticProxy.symbol,
                staticProxy.typeParameters.size,
                implFunRef.reflectionTarget, implFunRef.origin
            )
        }
        return implFunRef
    }

    private fun IrBuilderWithScope.irRawFunctionRef(irFun: IrFunction) =
        irRawFunctionReference(context.irBuiltIns.anyType, irFun.symbol)

    private fun IrBuilderWithScope.irVarargOfRawFunctionRefs(irFuns: List<IrFunction>) =
        irVararg(context.irBuiltIns.anyType, irFuns.map { irRawFunctionRef(it) })

    private inner class FunctionRichReferenceBuilder(val irFunctionReference: IrRichFunctionReference, val samSuperType: IrType? = null) {
        private val callee = irFunctionReference.invokeFunction
        private val isLambda = irFunctionReference.origin.isLambda
        private val isLightweightLambda = isLambda
                && shouldGenerateLightweightLambdas
                && !callee.hasAnnotation(JVM_SERIALIZABLE_LAMBDA_ANNOTATION_FQ_NAME)
        private val isHeavyweightLambda = isLambda && !isLightweightLambda
        private val isSuspend = irFunctionReference.overriddenFunctionSymbol.isSuspend

        // Only function references can bind a receiver and even then we can only bind either an extension or a dispatch receiver.
        // However, when we bind a value of an inline class type as a receiver, the receiver will turn into an argument of
        // the function in question. Yet we still need to record it as the "receiver" in CallableReference in order for reflection
        // to work correctly.
        private val boundReceivers: Map<IrValueParameter, IrExpression> =
            if (callee.isJvmStaticInObject()) mapOf(createFakeBoundReceiverForJvmStaticInObject())
            else (irFunctionReference.invokeFunction.parameters zip irFunctionReference.boundValues).toMap()

        // The type of the reference is KFunction<in A1, ..., in An, out R>
        private val parameterTypes = (irFunctionReference.type as IrSimpleType).arguments.map {
            when (it) {
                is IrTypeProjection -> it.type
                is IrStarProjection -> context.irBuiltIns.anyNType
            }
        }
        private val argumentTypes = parameterTypes.dropLast(1)

        private val nonCapturedParameters by lazy {
            irFunctionReference.invokeFunction.parameters.drop(irFunctionReference.boundValues.size)
        }
        private val overriddenMethodType = samSuperType?.erasedUpperBound?.rawType(context) ?: (
                if (irFunctionReference.invokeFunction.isSuspend) context.irBuiltIns.suspendFunctionN(argumentTypes.size).symbol
                else context.irBuiltIns.functionN(argumentTypes.size).symbol
                ).typeWith(parameterTypes)

        private val superMethod = irFunctionReference.overriddenFunctionSymbol.owner

        private val adapteeCall: IrFunctionAccessExpression? =
            if (callee.origin == IrDeclarationOrigin.ADAPTER_FOR_CALLABLE_REFERENCE) {
                // The body of a callable reference adapter contains either only a call, or an IMPLICIT_COERCION_TO_UNIT type operator
                // applied to a call. That call's target is the original function which we need to get owner/name/signature.
                val call = when (val statement = callee.body!!.statements.single()) {
                    is IrTypeOperatorCall -> {
                        assert(statement.operator == IrTypeOperator.IMPLICIT_COERCION_TO_UNIT) {
                            "Unexpected type operator in ADAPTER_FOR_CALLABLE_REFERENCE: ${callee.render()}"
                        }
                        statement.argument
                    }
                    is IrReturn -> statement.value
                    else -> statement
                }
                if (call !is IrFunctionAccessExpression) {
                    throw UnsupportedOperationException("Unknown structure of ADAPTER_FOR_CALLABLE_REFERENCE: ${callee.render()}")
                }
                call
            } else {
                null
            }

        private val adaptedReferenceOriginalTarget: IrFunction? = adapteeCall?.symbol?.owner
        private val isFunInterfaceConstructorReference =
            callee.origin == IrDeclarationOrigin.ADAPTER_FOR_FUN_INTERFACE_CONSTRUCTOR
        private val constructedFunInterfaceSymbol: IrClassSymbol? =
            if (isFunInterfaceConstructorReference)
                callee.returnType.classOrNull
                    ?: throw AssertionError("Fun interface type expected: ${callee.returnType.render()}")
            else
                null
        private val isAdaptedReference =
            isFunInterfaceConstructorReference || adaptedReferenceOriginalTarget != null

        private val samInterface = samSuperType?.getClass()
        private val isKotlinFunInterface = samInterface != null && !samInterface.isFromJava()

        private val needToGenerateSamEqualsHashCodeMethods =
            (isKotlinFunInterface || isJavaSamConversionWithEqualsHashCode) &&
                    (isAdaptedReference || !isLambda)

        private val superType =
            runIf(samSuperType == null) {
                when {
                    isLightweightLambda -> context.irBuiltIns.anyClass
                    isHeavyweightLambda -> context.symbols.lambdaClass
                    isFunInterfaceConstructorReference -> context.symbols.funInterfaceConstructorReferenceClass
                    else -> when {
                        isAdaptedReference -> context.symbols.adaptedFunctionReference
                        else -> context.symbols.functionReferenceImpl
                    }
                }.defaultType
            }

        private val functionReferenceClass = context.irFactory.buildClass {
            setSourceRange(irFunctionReference)
            visibility = DescriptorVisibilities.LOCAL
            // A callable reference results in a synthetic class, while a lambda is not synthetic.
            // We don't produce GENERATED_SAM_IMPLEMENTATION, which is always synthetic.
            origin = if (isLambda) JvmLoweredDeclarationOrigin.LAMBDA_IMPL else JvmLoweredDeclarationOrigin.FUNCTION_REFERENCE_IMPL
            name = SpecialNames.NO_NAME_PROVIDED
        }.apply {
            parent = currentDeclarationParent ?: error("No current declaration parent at ${irFunctionReference.dump()}")
            superTypes = listOfNotNull(
                superType,
                overriddenMethodType,
                if (needToGenerateSamEqualsHashCodeMethods) context.symbols.functionAdapter.defaultType else null,
            )
            if (samInterface != null && origin == JvmLoweredDeclarationOrigin.LAMBDA_IMPL) {
                // Old back-end generates formal type parameters as in SAM supertype.
                // Here we create formal type parameters with same names and equivalent upper bounds.
                // We don't really perform any type substitutions within class body
                // (it's all fine as soon as we have required generic signatures and don't fail anywhere).
                // NB this would no longer matter if we generate SAM wrapper classes as synthetic.
                typeParameters = createFakeFormalTypeParameters(samInterface.typeParameters, this)
            }
            createThisReceiverParameter()
            copyAttributes(irFunctionReference)
            if (isHeavyweightLambda) {
                metadata = callee.metadata
            }
        }

        private fun createFakeFormalTypeParameters(sourceTypeParameters: List<IrTypeParameter>, irClass: IrClass): List<IrTypeParameter> {
            if (sourceTypeParameters.isEmpty()) return emptyList()

            val fakeTypeParameters = sourceTypeParameters.map {
                buildTypeParameter(irClass) {
                    updateFrom(it)
                    name = it.name
                }
            }
            val typeRemapper = IrTypeParameterRemapper(sourceTypeParameters.associateWith { fakeTypeParameters[it.index] })
            for (fakeTypeParameter in fakeTypeParameters) {
                val sourceTypeParameter = sourceTypeParameters[fakeTypeParameter.index]
                fakeTypeParameter.superTypes = sourceTypeParameter.superTypes.map { typeRemapper.remapType(it) }
            }

            return fakeTypeParameters
        }

        fun build(): IrExpression = context.createJvmIrBuilder(currentScope!!).run {
            irBlock(irFunctionReference.startOffset, irFunctionReference.endOffset) {
                val constructor = createConstructor()
                val boundReceiverVars = irFunctionReference.boundValues.map(::irTemporary)
                createInvokeMethod(boundReceiverVars)

                if (needToGenerateSamEqualsHashCodeMethods) {
                    generateSamEqualsHashCodeMethods(boundReceiverVars)
                }
                if (isKotlinFunInterface) {
                    functionReferenceClass.addFakeOverrides(
                        backendContext.typeSystem,
                        buildMap {
                            samInterface?.declarationsAtFunctionReferenceLowering?.let { put(samInterface, it) }
                        }
                    )
                }

                +functionReferenceClass
                +irCall(constructor.symbol).apply {
                    if (constructor.parameters.isNotEmpty()) arguments.assignFrom(boundReceiverVars, ::irGet)
                }
            }
        }

        private fun JvmIrBuilder.generateSamEqualsHashCodeMethods(boundReceiverVars: List<IrVariable>) {
            checkNotNull(samSuperType) { "equals/hashCode can only be generated for fun interface wrappers: ${callee.render()}" }

            SamEqualsHashCodeMethodsGenerator(backendContext, functionReferenceClass, samSuperType) {
                val internalClass = when {
                    isAdaptedReference -> backendContext.symbols.adaptedFunctionReference
                    else -> backendContext.symbols.functionReferenceImpl
                }
                val constructor = internalClass.owner.constructors.single {
                    // arity, [receivers], owner, name, signature, flags
                    it.parameters.size == 1 + boundReceivers.size + 4
                }
                irCallConstructor(constructor.symbol, emptyList()).apply {
                    generateConstructorCallArguments(this) { irGet(boundReceiverVars[it]) }
                }
            }.generate()
        }

        private fun createConstructor(): IrConstructor =
            functionReferenceClass.addConstructor {
                origin = JvmLoweredDeclarationOrigin.GENERATED_MEMBER_IN_CALLABLE_REFERENCE
                returnType = functionReferenceClass.defaultType
                isPrimary = true
            }.apply {
                if (samSuperType == null) {
                    for (index in boundReceivers.entries.indices) {
                        addValueParameter("receiver$index", context.irBuiltIns.anyNType)
                    }
                }

                // Super constructor:
                // - For fun interface constructor references, super class is kotlin.jvm.internal.FunInterfaceConstructorReference
                //   with single constructor 'public FunInterfaceConstructorReference(Class funInterface)'
                // - For SAM references, the super class is Any
                // - For lambdas, accepts arity
                // - For optimized function references (1.4+), accepts:
                //       arity, [receiver], owner, name, signature, flags
                // - For unoptimized function references, accepts:
                //       arity, [receiver]
                val constructor =
                    when {
                        isFunInterfaceConstructorReference ->
                            context.symbols.funInterfaceConstructorReferenceClass.owner.constructors.single()
                        samSuperType != null ->
                            context.irBuiltIns.anyClass.owner.constructors.single()
                        else -> {
                            val expectedArity =
                                if (isLightweightLambda && !isAdaptedReference) 0
                                else if (isHeavyweightLambda && !isAdaptedReference) 1
                                else 1 + boundReceivers.size + 4
                            superType?.getClass()!!.constructors.single {
                                it.parameters.size == expectedArity
                            }
                        }
                    }

                body = context.createJvmIrBuilder(symbol).run {
                    irBlockBody(startOffset, endOffset) {
                        +irDelegatingConstructorCall(constructor).also { call ->
                            if (samSuperType == null) {
                                generateConstructorCallArguments(call) { irGet(parameters.first()) }
                            }
                        }
                        +IrInstanceInitializerCallImpl(startOffset, endOffset, functionReferenceClass.symbol, context.irBuiltIns.unitType)
                    }
                }
            }

        private fun JvmIrBuilder.generateConstructorCallArguments(
            call: IrFunctionAccessExpression,
            generateBoundReceiver: IrBuilder.(Int) -> IrExpression,
        ) {
            if (isFunInterfaceConstructorReference) {
                val funInterfaceKClassRef = kClassReference(constructedFunInterfaceSymbol!!.owner.defaultType)
                val funInterfaceJavaClassRef = kClassToJavaClass(funInterfaceKClassRef)
                call.arguments[0] = funInterfaceJavaClassRef
            } else {
                var index = 0
                if (!isLightweightLambda) {
                    call.arguments[index++] = irInt(nonCapturedParameters.size + if (isSuspend) 1 else 0)
                }
                for (it in boundReceivers.entries.indices) {
                    call.arguments[index++] = generateBoundReceiver(it)
                }
                if (!isLambda) {
                    val callableReferenceTarget = adaptedReferenceOriginalTarget
                        ?: irFunctionReference.reflectionTargetSymbol?.owner
                        ?: callee
                    val owner = calculateOwnerKClass(callableReferenceTarget.parent)
                    call.arguments[index++] = kClassToJavaClass(owner)
                    call.arguments[index++] = irString(callableReferenceTarget.originalName.asString())
                    call.arguments[index++] = generateSignature(callableReferenceTarget.symbol)
                    call.arguments[index] = irInt(getFunctionReferenceFlags(callableReferenceTarget))
                }
            }
        }

        private fun getFunctionReferenceFlags(callableReferenceTarget: IrFunction): Int {
            val isTopLevelBit = callableReferenceTarget.getCallableReferenceTopLevelFlag()
            val adaptedCallableReferenceFlags = getAdaptedCallableReferenceFlags()
            return isTopLevelBit + (adaptedCallableReferenceFlags shl 1)
        }

        private fun getAdaptedCallableReferenceFlags(): Int {
            if (adaptedReferenceOriginalTarget == null) return 0

            val isVarargMappedToElementBit = if (hasVarargMappedToElement()) 1 else 0
            val isSuspendConvertedBit = if (!adaptedReferenceOriginalTarget.isSuspend && callee.isSuspend) 1 else 0
            val isCoercedToUnitBit = if (!adaptedReferenceOriginalTarget.returnType.isUnit() && callee.returnType.isUnit()) 1 else 0

            return isVarargMappedToElementBit +
                    (isSuspendConvertedBit shl 1) +
                    (isCoercedToUnitBit shl 2)
        }

        private fun hasVarargMappedToElement(): Boolean =
            adapteeCall?.arguments.orEmpty().any { arg -> arg is IrVararg && arg.elements.any { it is IrGetValue } }

        private fun createInvokeMethod(
            boundValues: List<IrValueDeclaration>
        ): IrSimpleFunction {
            val superFunction = irFunctionReference.overriddenFunctionSymbol.owner
            val invokeFunction = irFunctionReference.invokeFunction
            val isLambda = irFunctionReference.origin.isLambda
            return functionReferenceClass.addFunction {
                setSourceRange(if (isLambda) invokeFunction else irFunctionReference)
                name = superFunction.name
                returnType = invokeFunction.returnType
                isOperator = superFunction.isOperator
                isSuspend = superFunction.isSuspend
            }.apply {
                annotations = invokeFunction.annotations
                metadata = functionReferenceClass.metadata

                parameters += createDispatchReceiverParameterWithClassParent()
                require(superFunction.typeParameters.isEmpty()) { "Fun interface abstract function can't have type parameters" }

                // todo should be erased if SAM?
                val typeSubstitutor = IrTypeSubstitutor(
                    extractTypeParameters(irFunctionReference.type.classOrFail.owner).map { it.symbol },
                    (irFunctionReference.type as IrSimpleType).arguments,
                    allowEmptySubstitution = true
                )

                val nonDispatchParameters = superFunction.nonDispatchParameters.mapIndexed { i, superParameter ->
                    val oldParameter = invokeFunction.parameters[i + irFunctionReference.boundValues.size]
                    superParameter.copyTo(
                        this,
                        startOffset = if (isLambda) oldParameter.startOffset else UNDEFINED_OFFSET,
                        endOffset = if (isLambda) oldParameter.endOffset else UNDEFINED_OFFSET,
                        name = oldParameter.name,
                        type = typeSubstitutor.substitute(superParameter.type),
                        defaultValue = null,
                    ).apply { copyAnnotationsFrom(oldParameter) }
                }
                this.parameters += nonDispatchParameters
                overriddenSymbols += superFunction.symbol

                val builder = context.createIrBuilder(symbol).applyIf(isLambda) { at(invokeFunction.body!!) }
                body = builder.irBlockBody {
                    val variablesMapping = buildMap {
                        for ((index, capturedValueDeclaration) in boundValues.withIndex()) {
                            put(invokeFunction.parameters[index], capturedValueDeclaration)
                        }
                        for ((index, parameter) in nonDispatchParameters.withIndex()) {
                            val invokeParameter = invokeFunction.parameters[index + boundValues.size]
                            if (parameter.type != invokeParameter.type) {
                                put(invokeParameter, irTemporary(irGet(parameter).implicitCastTo(invokeParameter.type)))
                            } else {
                                put(invokeParameter, parameter)
                            }
                        }
                    }
                    val transformedBody = invokeFunction.body!!.transform(object : VariableRemapper(variablesMapping) {
                        override fun visitReturn(expression: IrReturn): IrExpression {
                            if (expression.returnTargetSymbol == invokeFunction.symbol) {
                                expression.returnTargetSymbol = this@apply.symbol
                            }
                            return super.visitReturn(expression)
                        }

                        override fun visitDeclaration(declaration: IrDeclarationBase): IrStatement {
                            if (declaration.parent == invokeFunction)
                                declaration.parent = this@apply
                            return super.visitDeclaration(declaration)
                        }
                    }, null)
                    when (transformedBody) {
                        is IrBlockBody -> +transformedBody.statements
                        is IrExpressionBody -> +irReturn(transformedBody.expression)
                        else -> error("Unexpected body type: ${transformedBody::class.simpleName}")
                    }
                }
            }
        }

        // Inline the body of an anonymous function into the generated lambda subclass.
        private fun IrSimpleFunction.createLambdaInvokeMethod() {
            annotations += callee.annotations
            val valueParameterMap = callee.parameters.associate { param ->
                param to param.copyTo(this, kind = IrParameterKind.Regular)
            }
            parameters += valueParameterMap.values
            body = callee.moveBodyTo(this, valueParameterMap)
        }

        private fun IrSimpleFunction.createFunInterfaceConstructorInvokeMethod() {
            val adapterValueParameter = callee.parameters.singleOrNull()
                ?: throw AssertionError("Single value parameter expected: ${callee.render()}")
            val invokeValueParameter = adapterValueParameter.copyTo(this)
            val valueParameterMap = mapOf(adapterValueParameter to invokeValueParameter)
            parameters += invokeValueParameter
            body = callee.moveBodyTo(this, valueParameterMap)
            callee.body = null
        }

        private fun IrSimpleFunction.createFunctionReferenceInvokeMethod(receivers: List<IrValueDeclaration>) {
            for ((index, argumentType) in argumentTypes.withIndex()) {
                addValueParameter {
                    name = Name.identifier("p$index")
                    type = argumentType
                }
            }

            body = context.createJvmIrBuilder(symbol, startOffset, endOffset).irBlockBody {
                +irFunctionReference.invokeFunction
                var unboundIndex = 0
                val args = callee.parameters.map { parameter ->
                    val boundArgument = boundReceivers[parameter]
                    when {
                        boundArgument != null ->
                            // Bound receiver parameter. For function references, this is stored in a field of the superclass.
                            // For sam references, we just capture the value in a local variable and LocalDeclarationsLowering
                            // will put it into a field.
                            if (samSuperType == null)
                                irImplicitCast(
                                    irGetField(
                                        irGet(dispatchReceiverParameter!!),
                                        functionReferenceClass.getReceiverField(this@FunctionReferenceLowering.context)
                                    ),
                                    boundArgument.type
                                )
                            else
                                irGet(receivers.single()) // todo

                        unboundIndex >= nonCapturedParameters.size ->
                            // Default value argument (this pass doesn't handle suspend functions, otherwise
                            // it could also be the continuation argument)
                            null

                        else ->
                            irGet(nonDispatchParameters[unboundIndex++])
                    }
                }
                val call = irCall(callee.symbol, callee.returnType).apply {
                    arguments.assignFrom(args)
                }
                +irReturn(inlineAdapterCallIfPossible(call, this@createFunctionReferenceInvokeMethod))
            }
        }

        private fun inlineAdapterCallIfPossible(
            expression: IrFunctionAccessExpression,
            invokeMethod: IrSimpleFunction,
        ): IrExpression {
            val irCall = expression as? IrCall
                ?: return expression
            val callee = irCall.symbol.owner
            if (callee.origin != IrDeclarationOrigin.ADAPTER_FOR_CALLABLE_REFERENCE)
                return expression

            // TODO fix testSuspendUnitConversion
            if (callee.isSuspend) return expression

            // Callable reference adapter is a simple function that delegates to callable reference target,
            // adapting its signature for required functional type.
            // Usually it simply forwards arguments to target function.
            // It also passes 'receiver' field for bound references, with downcast to the actual receiver type.
            // In any case, adapter itself is synthetic and is not necessarily debuggable, so we can reuse variables freely.
            // Inlining adapter into 'invoke' saves us two methods (adapter & synthetic accessor).
            val adapterBody = callee.body as? IrBlockBody
            if (adapterBody == null || adapterBody.statements.size != 1)
                throw AssertionError("Unexpected adapter body: ${callee.dump()}")
            val resultStatement = adapterBody.statements[0]
            val resultExpression: IrExpression =
                when {
                    resultStatement is IrReturn ->
                        resultStatement.value
                    resultStatement is IrTypeOperatorCall && resultStatement.operator == IrTypeOperator.IMPLICIT_COERCION_TO_UNIT ->
                        resultStatement
                    resultStatement is IrCall ->
                        resultStatement
                    resultStatement is IrConstructorCall ->
                        resultStatement
                    else ->
                        throw AssertionError("Unexpected adapter body: ${callee.dump()}")
                }

            val startOffset = irCall.startOffset
            val endOffset = irCall.endOffset

            val callArguments = LinkedHashMap<IrValueParameter, IrValueDeclaration>()
            val inlinedAdapterBlock = IrBlockImpl(startOffset, endOffset, irCall.type, origin = null)
            var tmpVarIndex = 0

            fun wrapIntoTemporaryVariableIfNecessary(expression: IrExpression): IrValueDeclaration {
                if (expression is IrGetValue)
                    return expression.symbol.owner
                if (expression !is IrTypeOperatorCall || expression.argument !is IrGetField)
                    throw AssertionError("Unexpected adapter argument:\n${expression.dump()}")
                val temporaryVar = IrVariableImpl(
                    startOffset, endOffset, IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
                    IrVariableSymbolImpl(),
                    Name.identifier("tmp_${tmpVarIndex++}"),
                    expression.type,
                    isVar = false, isConst = false, isLateinit = false
                )
                temporaryVar.parent = invokeMethod
                temporaryVar.initializer = expression
                inlinedAdapterBlock.statements.add(temporaryVar)
                return temporaryVar
            }

            for (parameter in callee.parameters) {
                callArguments[parameter] = wrapIntoTemporaryVariableIfNecessary(
                    irCall.arguments[parameter]
                        ?: throw AssertionError("No value argument #${parameter.indexInParameters} in adapter call: ${irCall.dump()}")
                )
            }

            val inlinedAdapterResult = resultExpression.transform(VariableRemapper(callArguments), null)
            inlinedAdapterBlock.statements.add(inlinedAdapterResult)

            callee.body = null
            return inlinedAdapterBlock.patchDeclarationParents(invokeMethod)
        }

        private val IrFunction.originalName: Name
            get() = metadata?.name ?: name

        private fun JvmIrBuilder.generateSignature(target: IrFunctionSymbol): IrExpression =
            irCall(backendContext.symbols.signatureStringIntrinsic).apply {
                arguments[0] = IrRawFunctionReferenceImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET, irFunctionReference.type, target
                )
            }

        private fun createFakeBoundReceiverForJvmStaticInObject(): Pair<IrValueParameter, IrGetObjectValueImpl> {
            // JvmStatic functions in objects are special in that they are generated as static methods in the bytecode, and JVM IR lowers
            // both declarations and call sites early on in jvmStaticInObjectPhase because it's easier that way in subsequent lowerings.
            // However from the point of view of Kotlin language (and thus reflection), these functions still take the dispatch receiver
            // parameter of the object type. So we pretend here that a JvmStatic function in object has an additional dispatch receiver
            // parameter, so that the correct function reference object will be created and reflective calls will work at runtime.
            val objectClass = callee.parentAsClass
            return buildValueParameter(callee) {
                name = Name.identifier("\$this")
                type = objectClass.typeWith()
            } to IrGetObjectValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, objectClass.typeWith(), objectClass.symbol)
        }
    }

    companion object {
        internal fun JvmIrBuilder.calculateOwnerKClass(irContainer: IrDeclarationParent): IrExpression =
            kClassReference(irContainer.getCallableReferenceOwnerKClassType(backendContext))

        // This method creates an IrField for the field `kotlin.jvm.internal.CallableReference.receiver`, as if this field was declared
        // in the anonymous class for this callable reference. Technically it's incorrect because this field is not declared here. It would
        // be more correct to create a fake override but that seems like more work for no clear benefit. Codegen will generate the correct
        // field access anyway, even if the field is not present in this parent.
        // Note that it is necessary to generate an access to the field whose parent is this anonymous class, and NOT some supertype like
        // k.j.i.CallableReference, or k.j.i.FunctionReferenceImpl, because then AddSuperQualifierToJavaFieldAccess lowering would add
        // superQualifierSymbol, which would break inlining of bound function references, since inliner will not understand how to transform
        // this getfield instruction in the bytecode.
        internal fun IrClass.getReceiverField(context: JvmBackendContext): IrField =
            context.irFactory.buildField {
                name = Name.identifier("receiver")
                type = context.irBuiltIns.anyNType
                visibility = DescriptorVisibilities.PROTECTED
            }.apply {
                parent = this@getReceiverField
            }
    }
}

data class IndyCallData(val shouldBeSerializable: Boolean)

var IrRichFunctionReference.indyCallData by irAttribute<_, IndyCallData>(copyByDefault = true)
    private set
