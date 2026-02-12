/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.LAMBDA_EXTENSION_RECEIVER
import org.jetbrains.kotlin.backend.common.lower.SamEqualsHashCodeMethodsGenerator
import org.jetbrains.kotlin.backend.common.lower.VariableRemapper
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.declarationsAtFunctionReferenceLowering
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.JvmLoweredStatementOrigin
import org.jetbrains.kotlin.backend.jvm.ir.*
import org.jetbrains.kotlin.backend.jvm.lower.indy.*
import org.jetbrains.kotlin.config.JvmClosureGenerationScheme
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrStarProjection
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.JvmStandardClassIds.JVM_SERIALIZABLE_LAMBDA_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.applyIf
import org.jetbrains.kotlin.utils.addToStdlib.assignFrom
import org.jetbrains.kotlin.utils.addToStdlib.runIf


private sealed class BoundValue {
    class StoredInVariable(val symbol: IrVariable) : BoundValue()
    class StoredInField(val symbol: IrField): BoundValue()
}

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

    private fun IrRichFunctionReference.isSamConversion(): Boolean =
        !type.isFunctionOrKFunction() && !type.isSuspendFunctionOrKFunction()

    override fun visitRichFunctionReference(expression: IrRichFunctionReference): IrExpression {
        expression.transformChildrenVoid(this)
        if (expression.isIgnored) return expression
        for (parameter in expression.invokeFunction.parameters) {
            // this origin affects how name in bytecode would be generated. We need this change only for inline lambdas,
            // so let's drop it for everything else.
            if (parameter.origin == LAMBDA_EXTENSION_RECEIVER) {
                parameter.origin = IrDeclarationOrigin.DEFINED
            }
        }
        if (shouldGenerateIndyLambdas && expression.origin.isLambda && !expression.isSamConversion()) {
            if (setIndyDataIfPossible(expression, plainLambda = true)) return expression
        }
        if (shouldGenerateIndySamConversions && expression.isSamConversion()) {
            if (setIndyDataIfPossible(expression, plainLambda = false)) return expression
        }

        val samSuperType = runIf(expression.isSamConversion()) { expression.type.erasedUpperBound.rawType(context) }

        return FunctionReferenceBuilder(expression, samSuperType).build()
    }

    override fun visitClassNew(declaration: IrClass): IrStatement {
        if (declaration.isFun || declaration.symbol.isSuspendFunction() || declaration.symbol.isKSuspendFunction()) {
            declaration.declarationsAtFunctionReferenceLowering = declaration.declarations.toList()
        }
        declaration.transformChildrenVoid()
        return declaration
    }

    private fun setIndyDataIfPossible(expression: IrRichFunctionReference, plainLambda: Boolean): Boolean {
        val lambdaMetafactoryArguments =
            LambdaMetafactoryArgumentsBuilder(context, crossinlineLambdas)
                .getLambdaMetafactoryArguments(expression, plainLambda = plainLambda, forceSerializability = false)
        return if (lambdaMetafactoryArguments is LambdaMetafactoryArguments) {
            expression.indyCallData = IndyCallData(lambdaMetafactoryArguments.shouldBeSerializable, plainLambda)
            true
        } else {
            false
        }
    }


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

    // Handle SAM conversions which wrap neither a function reference nor a lambda.
    // The case of function reference or lambda was already covered by [UpgradeCallableReferenceLowering]
    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
        expression.transformChildrenVoid()
        if (expression.operator != IrTypeOperator.SAM_CONVERSION) {
            return expression
        }

        val samSuperType = expression.typeOperand
        val invokable = expression.argument

        if (shouldGenerateIndySamConversions && canGenerateIndySamConversionOnFunctionalExpression(samSuperType)) {
            val lambdaBlock = SamDelegatingLambdaBuilder(context)
                .build(invokable, samSuperType, currentScope!!.scope.scopeOwnerSymbol, getDeclarationParentForDelegatingLambda())

            // Do not convert if it's not possible to do with indy.
            // This case is handled more optimally by SingleAbstractMethodLowering
            if (setIndyDataIfPossible(lambdaBlock.ref, plainLambda = false)) {
                if (lambdaBlock.ref.indyCallData?.forceSerializability == true) {
                    if (invokable is IrTypeOperatorCall && invokable.operator == IrTypeOperator.IMPLICIT_CAST) {
                        val argument = invokable.argument
                        if (argument is IrRichFunctionReference) {
                            argument.indyCallData = argument.indyCallData?.copy(forceSerializability = true)
                        }
                    }
                }
                return lambdaBlock.rootExpression
            }
        }
        return expression
    }

    private fun canGenerateIndySamConversionOnFunctionalExpression(samSuperType: IrType): Boolean {
        val samClass = samSuperType.classOrNull
            ?: throw AssertionError("Class type expected: ${samSuperType.render()}")
        return samClass.owner.isFromJava() && !isJavaSamConversionWithEqualsHashCode
    }

    private inner class FunctionReferenceBuilder(val irFunctionReference: IrRichFunctionReference, val samSuperType: IrType? = null) {
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

        private val adaptedReferenceOriginalTarget: IrFunction? = irFunctionReference.reflectionTargetSymbol?.owner?.takeIf {
            irFunctionReference.invokeFunction.origin == IrDeclarationOrigin.ADAPTER_FOR_CALLABLE_REFERENCE
        }
        private val isFunInterfaceConstructorReference =
            callee.origin == IrDeclarationOrigin.ADAPTER_FOR_FUN_INTERFACE_CONSTRUCTOR
        private val constructedFunInterfaceSymbol: IrClassSymbol? =
            if (isFunInterfaceConstructorReference)
                callee.returnType.classOrNull
                    ?: throw AssertionError("Fun interface type expected: ${callee.returnType.render()}")
            else
                null
        private val isAdaptedReference =
            isFunInterfaceConstructorReference || callee.origin == IrDeclarationOrigin.ADAPTER_FOR_CALLABLE_REFERENCE

        private val samInterface = samSuperType?.getClass()
        private val isKotlinFunInterface = samInterface != null && !samInterface.isFromJava()

        private val needToGenerateSamEqualsHashCodeMethods =
            (isKotlinFunInterface || isJavaSamConversionWithEqualsHashCode) &&
                    (isAdaptedReference || !isLambda)

        private val superClass =
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
            superTypes = buildList {
                addIfNotNull(this@FunctionReferenceBuilder.superClass)
                if (samSuperType != null) {
                    add(samSuperType)
                } else {
                    // we still need FunctionN if irFunctionReference.type is KFunctionN
                    val functionClassSymbol = if (irFunctionReference.invokeFunction.isSuspend) {
                        context.irBuiltIns.suspendFunctionN(argumentTypes.size).symbol
                    } else {
                        context.irBuiltIns.functionN(argumentTypes.size).symbol
                    }
                    add(functionClassSymbol.typeWith(parameterTypes))
                }
                if (needToGenerateSamEqualsHashCodeMethods) {
                    add(context.symbols.functionAdapter.defaultType)
                }
            }
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
                require(irFunctionReference.boundValues.size <= 1) { "Function references with multiple bound values are not supported yet" }
                +functionReferenceClass

                // For function references the bound receiver parameter is stored in a field of the superclass.
                // For sam references, we just capture the value in a local variable, and LocalDeclarationsLowering
                // will put it into a field.
                if (samSuperType != null) {
                    val boundValues = irFunctionReference.boundValues.map { BoundValue.StoredInVariable(irTemporary(it)) }
                    createInvokeMethod(boundValues)
                    if (needToGenerateSamEqualsHashCodeMethods) {
                        generateSamEqualsHashCodeMethods(boundValues)
                    }
                    +irCall(constructor.symbol)
                } else {
                    val boundValues = irFunctionReference.boundValues.map {
                        BoundValue.StoredInField(functionReferenceClass.getReceiverField(backendContext))
                    }
                    createInvokeMethod(boundValues)
                    +irCall(constructor.symbol).apply {
                        arguments.assignFrom(irFunctionReference.boundValues)
                    }
                }

                if (isKotlinFunInterface) {
                    functionReferenceClass.addFakeOverrides(
                        backendContext.typeSystem,
                        buildMap {
                            samInterface?.declarationsAtFunctionReferenceLowering?.let { put(samInterface, it) }
                        }
                    )
                }
            }
        }

        private fun JvmIrBuilder.generateSamEqualsHashCodeMethods(boundReceiverVars: List<BoundValue.StoredInVariable>) {
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
                    generateConstructorCallArguments(this) { irGet(boundReceiverVars[it].symbol) }
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
                            superClass?.getClass()!!.constructors.single {
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
                    call.arguments[index++] = irInt(irFunctionReference.invokeFunction.parameters.size - irFunctionReference.boundValues.size + if (isSuspend) 1 else 0)
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

            val isVarargMappedToElementBit = if (irFunctionReference.hasVarargConversion) 1 else 0
            val isSuspendConvertedBit = if (irFunctionReference.hasSuspendConversion) 1 else 0
            val isCoercedToUnitBit = if (irFunctionReference.hasUnitConversion) 1 else 0

            return isVarargMappedToElementBit +
                    (isSuspendConvertedBit shl 1) +
                    (isCoercedToUnitBit shl 2)
        }

        /**
         * This function is very similar to [org.jetbrains.kotlin.backend.common.lower.AbstractFunctionReferenceLowering.buildInvokeMethod].
         * If you make any changes, don't forget to also change the other one.
         */
        private fun createInvokeMethod(
            boundValues: List<BoundValue>
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
                when (irFunctionReference.origin) {
                    JvmLoweredStatementOrigin.CALLABLE_REFERENCE_AS_INLINABLE_DEFAULT_VALUE ->
                        origin = JvmLoweredDeclarationOrigin.INVOKE_OF_CALLABLE_REFERENCE_AS_INLINABLE_DEFAULT_VALUE
                    JvmLoweredStatementOrigin.SUSPEND_LAMBDA_AS_INLINABLE_DEFAULT_VALUE ->
                        origin = JvmLoweredDeclarationOrigin.INVOKE_OF_SUSPEND_LAMBDA_AS_INLINABLE_DEFAULT_VALUE
                }
                annotations = invokeFunction.annotations
                metadata = functionReferenceClass.metadata

                parameters += createDispatchReceiverParameterWithClassParent()
                require(superFunction.typeParameters.isEmpty()) { "Fun interface abstract function can't have type parameters" }

                val typeSubstitutor = if (samSuperType == null) {
                    IrTypeSubstitutor(
                        extractTypeParameters(irFunctionReference.type.classOrFail.owner).map { it.symbol },
                        (irFunctionReference.type as IrSimpleType).arguments.map {
                            when (it) {
                                // it can be "in Nothing" in case of strange intersections
                                // if we keep it as Nothing, this can cause CCE on java.lang.Void
                                // this happens only in K1 in tests, so maybe it's just a K1 bug, but let's be conservative in that case–
                                is IrTypeProjection if it.type.isNothing() -> context.irBuiltIns.anyNType
                                is IrTypeProjection -> it.type
                                is IrStarProjection -> context.irBuiltIns.anyNType
                            }
                        },
                        allowEmptySubstitution = true
                    )
                } else {
                    val typeParameters = extractTypeParameters(samSuperType.classOrFail.owner)
                    IrTypeSubstitutor(
                        typeParameters.map { it.symbol },
                        typeParameters.map { it.erasedUpperBound.rawType(context) },
                        allowEmptySubstitution = true
                    )
                }

                val nonDispatchParameters = superFunction.nonDispatchParameters.mapIndexed { i, superParameter ->
                    val oldParameter = invokeFunction.parameters[i + irFunctionReference.boundValues.size]
                    superParameter.copyTo(
                        this,
                        startOffset = if (isLambda) oldParameter.startOffset else UNDEFINED_OFFSET,
                        endOffset = if (isLambda) oldParameter.endOffset else UNDEFINED_OFFSET,
                        name = oldParameter.name,
                        origin = oldParameter.origin,
                        type = typeSubstitutor.substitute(superParameter.type)
                            .mergeNullability(invokeFunction.parameters[i + boundValues.size].type),
                        defaultValue = null,
                    ).apply { copyAnnotationsFrom(oldParameter) }
                }
                this.parameters += nonDispatchParameters
                overriddenSymbols += superFunction.symbol

                val builder = context.createIrBuilder(symbol).applyIf(isLambda) { at(invokeFunction.body!!) }
                body = builder.irBlockBody {
                    val variablesMapping = buildMap {
                        for ((index, capturedValue) in boundValues.withIndex()) {
                            val invokeParameter = invokeFunction.parameters[index]
                            val capturedValueLocal = when (capturedValue) {
                                is BoundValue.StoredInVariable -> capturedValue.symbol
                                is BoundValue.StoredInField -> irTemporary(
                                    irImplicitCast(
                                        irGetField(
                                            irGet(dispatchReceiverParameter!!),
                                            capturedValue.symbol
                                        ),
                                        invokeParameter.type,
                                    )
                                )
                            }
                            put(invokeParameter, capturedValueLocal)
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

data class IndyCallData(
    val forceSerializability: Boolean,
    val plainLambda: Boolean
)

var IrRichFunctionReference.indyCallData by irAttribute<_, IndyCallData>(copyByDefault = true)
    private set
