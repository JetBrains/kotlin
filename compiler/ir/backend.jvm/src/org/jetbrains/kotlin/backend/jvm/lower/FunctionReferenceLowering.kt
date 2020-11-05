/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ir.*
import org.jetbrains.kotlin.backend.common.lower.SamEqualsHashCodeMethodsGenerator
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.ir.*
import org.jetbrains.kotlin.backend.jvm.lower.inlineclasses.InlineClassAbi
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

internal val functionReferencePhase = makeIrFilePhase(
    ::FunctionReferenceLowering,
    name = "FunctionReference",
    description = "Construct instances of anonymous KFunction subclasses for function references"
)

internal class FunctionReferenceLowering(private val context: JvmBackendContext) : FileLoweringPass, IrElementTransformerVoidWithContext() {
    // This pass ignores function references used as inline arguments. `InlineCallableReferenceToLambdaPhase`
    // converts them into lambdas instead, so that after inlining there is only a direct call left, with no
    // function reference classes needed.
    private val ignoredFunctionReferences = mutableSetOf<IrCallableReference<*>>()

    private val IrFunctionReference.isIgnored: Boolean
        get() = (!type.isFunctionOrKFunction() && !isSuspendFunctionReference()) || ignoredFunctionReferences.contains(this)

    // `suspend` function references are the same as non-`suspend` ones, just with a `suspend` invoke;
    // however, suspending lambdas require different generation implemented in AddContinuationLowering
    // because they are also their own continuation classes.
    // TODO: Currently, origin of callable references explicitly written in source code is null. Do we need to create one?
    private fun IrFunctionReference.isSuspendFunctionReference(): Boolean = isSuspend &&
            (origin == null || origin == IrStatementOrigin.ADAPTED_FUNCTION_REFERENCE || origin == IrStatementOrigin.SUSPEND_CONVERSION)

    override fun lower(irFile: IrFile) {
        ignoredFunctionReferences.addAll(IrInlineReferenceLocator.scan(context, irFile))
        irFile.transformChildrenVoid(this)
    }

    override fun visitBlock(expression: IrBlock): IrExpression {
        if (!expression.origin.isLambda)
            return super.visitBlock(expression)

        val reference = expression.statements.last() as IrFunctionReference
        if (reference.isIgnored)
            return super.visitBlock(expression)

        expression.statements.dropLast(1).forEach { it.transform(this, null) }
        reference.transformChildrenVoid(this)
        return FunctionReferenceBuilder(reference).build()
    }

    override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
        expression.transformChildrenVoid(this)
        return if (expression.isIgnored) expression else FunctionReferenceBuilder(expression).build()
    }

    // Handle SAM conversions which wrap a function reference:
    //     class sam$n(private val receiver: R) : Interface { override fun method(...) = receiver.target(...) }
    //
    // This avoids materializing an invokable KFunction representing, thus producing one less class.
    // This is actually very common, as `Interface { something }` is a local function + a SAM-conversion
    // of a reference to it into an implementation.
    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
        if (expression.operator == IrTypeOperator.SAM_CONVERSION) {
            val invokable = expression.argument
            val reference = if (invokable is IrFunctionReference) {
                invokable
            } else if (invokable is IrBlock && invokable.origin.isLambda && invokable.statements.last() is IrFunctionReference) {
                invokable.statements.dropLast(1).forEach { it.transform(this, null) }
                invokable.statements.last() as IrFunctionReference
            } else {
                return super.visitTypeOperator(expression)
            }
            reference.transformChildrenVoid()
            return FunctionReferenceBuilder(reference, expression.typeOperand).build()
        }
        return super.visitTypeOperator(expression)
    }

    private inner class FunctionReferenceBuilder(val irFunctionReference: IrFunctionReference, val samSuperType: IrType? = null) {
        private val isLambda = irFunctionReference.origin.isLambda

        private val callee = irFunctionReference.symbol.owner

        // Only function references can bind a receiver and even then we can only bind either an extension or a dispatch receiver.
        // However, when we bind a value of an inline class type as a receiver, the receiver will turn into an argument of
        // the function in question. Yet we still need to record it as the "receiver" in CallableReference in order for reflection
        // to work correctly.
        private val boundReceiver: Pair<IrValueParameter, IrExpression>? = irFunctionReference.getArgumentsWithIr().singleOrNull()

        // The type of the reference is KFunction<in A1, ..., in An, out R>
        private val parameterTypes = (irFunctionReference.type as IrSimpleType).arguments.map { (it as IrTypeProjection).type }
        private val argumentTypes = parameterTypes.dropLast(1)

        private val typeArgumentsMap = irFunctionReference.typeSubstitutionMap

        private val functionSuperClass =
            samSuperType?.classOrNull
                ?: if (irFunctionReference.isSuspend)
                    context.ir.symbols.getJvmSuspendFunctionClass(argumentTypes.size)
                else
                    context.ir.symbols.getJvmFunctionClass(argumentTypes.size)
        private val superMethod =
            functionSuperClass.functions.single { it.owner.modality == Modality.ABSTRACT }

        private val useOptimizedSuperClass =
            context.state.generateOptimizedCallableReferenceSuperClasses

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
        private val isAdaptedReference = adaptedReferenceOriginalTarget != null

        private val isKotlinFunInterface =
            samSuperType != null && samSuperType.getClass()?.origin != IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB

        private val needToGenerateSamEqualsHashCodeMethods =
            isKotlinFunInterface && (isAdaptedReference || !isLambda)

        private val superType =
            samSuperType ?: when {
                isLambda -> context.ir.symbols.lambdaClass
                useOptimizedSuperClass -> when {
                    isAdaptedReference -> context.ir.symbols.adaptedFunctionReference
                    else -> context.ir.symbols.functionReferenceImpl
                }
                else -> context.ir.symbols.functionReference
            }.defaultType

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
                if (samSuperType == null)
                    functionSuperClass.typeWith(parameterTypes)
                else null,
                if (irFunctionReference.isSuspend)
                    context.ir.symbols.suspendFunctionInterface.defaultType
                else null,
                if (needToGenerateSamEqualsHashCodeMethods)
                    context.ir.symbols.functionAdapter.defaultType
                else null,
            )
            createImplicitParameterDeclarationWithWrappedDescriptor()
            copyAttributes(irFunctionReference)
            if (isLambda) {
                metadata = irFunctionReference.symbol.owner.metadata
            }
        }

        private val receiverField = context.ir.symbols.functionReferenceReceiverField.owner

        fun build(): IrExpression = context.createJvmIrBuilder(currentScope!!.scope.scopeOwnerSymbol).run {
            irBlock(irFunctionReference.startOffset, irFunctionReference.endOffset) {
                val constructor = createConstructor()
                val boundReceiverVar =
                    if (samSuperType != null && boundReceiver != null) {
                        irTemporary(boundReceiver.second)
                    } else null
                createInvokeMethod(boundReceiverVar)

                if (!isLambda && samSuperType == null && !useOptimizedSuperClass) {
                    createLegacyMethodOverride(irSymbols.functionReferenceGetSignature.owner) {
                        generateSignature(callee.symbol)
                    }
                    createLegacyMethodOverride(irSymbols.functionReferenceGetName.owner) {
                        irString(callee.originalName.asString())
                    }
                    createLegacyMethodOverride(irSymbols.functionReferenceGetOwner.owner) {
                        calculateOwner(callee.parent, backendContext)
                    }
                }

                if (needToGenerateSamEqualsHashCodeMethods) {
                    generateSamEqualsHashCodeMethods(boundReceiverVar)
                }
                if (isKotlinFunInterface) {
                    functionReferenceClass.addFakeOverrides(context.irBuiltIns)
                }

                +functionReferenceClass
                +irCall(constructor.symbol).apply {
                    if (valueArgumentsCount > 0) putValueArgument(0, boundReceiver!!.second)
                }
            }
        }

        private fun JvmIrBuilder.generateSamEqualsHashCodeMethods(boundReceiverVar: IrVariable?) {
            checkNotNull(samSuperType) { "equals/hashCode can only be generated for fun interface wrappers: ${callee.render()}" }

            if (!useOptimizedSuperClass) {
                // This is the case of a fun interface wrapper over a (maybe adapted) function reference,
                // with `-Xno-optimized-callable-referenced` enabled. We can't use constructors of FunctionReferenceImpl,
                // so we'd need to basically generate a full class for a reference inheriting from FunctionReference,
                // effectively disabling the optimization of fun interface wrappers over references.
                // This scenario is probably not very popular because it involves using equals/hashCode on function references
                // and enabling the mentioned internal compiler argument.
                // Right now we generate them as abstract so that any call would result in AbstractMethodError.
                // TODO: generate getFunctionDelegate, equals and hashCode properly in this case
                functionReferenceClass.addFunction("equals", backendContext.irBuiltIns.booleanType, Modality.ABSTRACT).apply {
                    addValueParameter("other", backendContext.irBuiltIns.anyNType)
                }
                functionReferenceClass.addFunction("hashCode", backendContext.irBuiltIns.intType, Modality.ABSTRACT)
                return
            }

            SamEqualsHashCodeMethodsGenerator(backendContext, functionReferenceClass, samSuperType) {
                val internalClass = when {
                    isAdaptedReference -> backendContext.ir.symbols.adaptedFunctionReference
                    else -> backendContext.ir.symbols.functionReferenceImpl
                }
                val constructor = internalClass.owner.constructors.single {
                    // arity, [receiver], owner, name, signature, flags
                    it.valueParameters.size == 1 + (if (boundReceiver != null) 1 else 0) + 4
                }
                irCallConstructor(constructor.symbol, emptyList()).apply {
                    generateConstructorCallArguments(this) { irGet(boundReceiverVar!!) }
                }
            }.generate()
        }

        private fun createConstructor(): IrConstructor =
            functionReferenceClass.addConstructor {
                origin = JvmLoweredDeclarationOrigin.GENERATED_MEMBER_IN_CALLABLE_REFERENCE
                returnType = functionReferenceClass.defaultType
                isPrimary = true
            }.apply {
                // Add receiver parameter for bound function references
                if (samSuperType == null) {
                    boundReceiver?.let { (param, arg) ->
                        valueParameters += param.copyTo(irFunction = this, index = 0, type = arg.type)
                    }
                }

                // Super constructor:
                // - For SAM references, the super class is Any
                // - For lambdas, accepts arity
                // - For optimized function references (1.4+), accepts:
                //       arity, [receiver], owner, name, signature, flags
                // - For unoptimized function references, accepts:
                //       arity, [receiver]
                val constructor = if (samSuperType != null) {
                    context.irBuiltIns.anyClass.owner.constructors.single()
                } else {
                    val expectedArity =
                        if (isLambda && !isAdaptedReference) 1
                        else 1 + (if (boundReceiver != null) 1 else 0) + (if (useOptimizedSuperClass) 4 else 0)
                    superType.getClass()!!.constructors.single {
                        it.valueParameters.size == expectedArity
                    }
                }

                body = context.createJvmIrBuilder(symbol).run {
                    irBlockBody(startOffset, endOffset) {
                        +irDelegatingConstructorCall(constructor).also { call ->
                            if (samSuperType == null) {
                                generateConstructorCallArguments(call) { irGet(valueParameters.first()) }
                            }
                        }
                        +IrInstanceInitializerCallImpl(startOffset, endOffset, functionReferenceClass.symbol, context.irBuiltIns.unitType)
                    }
                }
            }

        private fun JvmIrBuilder.generateConstructorCallArguments(
            call: IrFunctionAccessExpression,
            generateBoundReceiver: IrBuilder.() -> IrExpression
        ) {
            var index = 0
            call.putValueArgument(index++, irInt(argumentTypes.size + if (irFunctionReference.isSuspend) 1 else 0))
            if (boundReceiver != null) {
                call.putValueArgument(index++, generateBoundReceiver())
            }
            if (!isLambda && useOptimizedSuperClass) {
                val callableReferenceTarget = adaptedReferenceOriginalTarget ?: callee
                val owner = calculateOwnerKClass(callableReferenceTarget.parent, backendContext)
                call.putValueArgument(index++, kClassToJavaClass(owner, backendContext))
                call.putValueArgument(index++, irString(callableReferenceTarget.originalName.asString()))
                call.putValueArgument(index++, generateSignature(callableReferenceTarget.symbol))
                call.putValueArgument(index, irInt(getFunctionReferenceFlags(callableReferenceTarget)))
            }
        }

        private fun getFunctionReferenceFlags(callableReferenceTarget: IrFunction): Int {
            val isTopLevelBit = getCallableReferenceTopLevelFlag(callableReferenceTarget)
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

        private fun hasVarargMappedToElement(): Boolean {
            if (adapteeCall == null) return false
            for (i in 0 until adapteeCall.valueArgumentsCount) {
                val arg = adapteeCall.getValueArgument(i) ?: continue
                if (arg !is IrVararg) continue
                for (varargElement in arg.elements) {
                    if (varargElement is IrGetValue) return true
                }
            }
            return false
        }

        private fun createInvokeMethod(receiverVar: IrValueDeclaration?): IrSimpleFunction =
            functionReferenceClass.addFunction {
                setSourceRange(if (isLambda) callee else irFunctionReference)
                name = if (samSuperType == null && callee.returnType.erasedUpperBound.isInline && context.state.functionsWithInlineClassReturnTypesMangled) {
                    // For functions with inline class return type we need to mangle the invoke method.
                    // Otherwise, bridge lowering may fail to generate bridges for inline class types erasing to Any.
                    val suffix = InlineClassAbi.hashSuffix(callee, mangleReturnTypes = true, useOldMangleRules = false)
                    Name.identifier("${superMethod.owner.name.asString()}-${suffix}")
                } else superMethod.owner.name
                returnType = callee.returnType
                isSuspend = callee.isSuspend
            }.apply {
                overriddenSymbols += superMethod
                dispatchReceiverParameter = parentAsClass.thisReceiver!!.copyTo(this)
                if (isLambda) createLambdaInvokeMethod() else createFunctionReferenceInvokeMethod(receiverVar)
            }

        // Inline the body of an anonymous function into the generated lambda subclass.
        private fun IrSimpleFunction.createLambdaInvokeMethod() {
            annotations += callee.annotations
            val valueParameterMap = callee.explicitParameters.withIndex().associate { (index, param) ->
                param to param.copyTo(this, index = index)
            }
            valueParameters += valueParameterMap.values
            body = callee.moveBodyTo(this, valueParameterMap)
        }

        private fun IrSimpleFunction.createFunctionReferenceInvokeMethod(receiver: IrValueDeclaration?) {
            for ((index, argumentType) in argumentTypes.withIndex()) {
                addValueParameter {
                    name = Name.identifier("p$index")
                    type = argumentType
                }
            }

            body = context.createJvmIrBuilder(symbol, startOffset, endOffset).run {
                var unboundIndex = 0
                irExprBody(irCall(callee).apply {
                    for (typeParameter in irFunctionReference.symbol.owner.allTypeParameters) {
                        putTypeArgument(typeParameter.index, typeArgumentsMap[typeParameter.symbol])
                    }

                    for (parameter in callee.explicitParameters) {
                        when {
                            boundReceiver?.first == parameter ->
                                // Bound receiver parameter. For function references, this is stored in a field of the superclass.
                                // For sam references, we just capture the value in a local variable and LocalDeclarationsLowering
                                // will put it into a field.
                                if (samSuperType == null)
                                    irImplicitCast(
                                        irGetField(irGet(dispatchReceiverParameter!!),
                                                   this@FunctionReferenceBuilder.receiverField
                                        ),
                                        boundReceiver.second.type
                                    )
                                else
                                    irGet(receiver!!)

                            // If a vararg parameter corresponds to exactly one KFunction argument, which is an array, that array
                            // is forwarded as is.
                            //
                            //     fun f(x: (Int, Array<String>) -> String) = x(0, arrayOf("OK", "FAIL"))
                            //     fun h(i: Int, vararg xs: String) = xs[i]
                            //     f(::h)
                            //
                            parameter.isVararg && unboundIndex < argumentTypes.size && parameter.type == valueParameters[unboundIndex].type ->
                                irGet(valueParameters[unboundIndex++])
                            // In all other cases, excess arguments are packed into a new array.
                            //
                            //     fun g(x: (Int, String, String) -> String) = x(0, "OK", "FAIL")
                            //     f(::h) == g(::h)
                            //
                            parameter.isVararg && (unboundIndex < argumentTypes.size || !parameter.hasDefaultValue()) ->
                                irArray(parameter.type) {
                                    (unboundIndex until argumentTypes.size).forEach { +irGet(valueParameters[unboundIndex++]) }
                                }

                            unboundIndex >= argumentTypes.size ->
                                // Default value argument (this pass doesn't handle suspend functions, otherwise
                                // it could also be the continuation argument)
                                null

                            else ->
                                irGet(valueParameters[unboundIndex++])
                        }?.let { putArgument(callee, parameter, it) }
                    }
                })
            }
        }

        private fun buildOverride(superFunction: IrSimpleFunction, newReturnType: IrType = superFunction.returnType): IrSimpleFunction =
            functionReferenceClass.addFunction {
                setSourceRange(irFunctionReference)
                origin = JvmLoweredDeclarationOrigin.GENERATED_MEMBER_IN_CALLABLE_REFERENCE
                name = superFunction.name
                returnType = newReturnType
                visibility = superFunction.visibility
                isSuspend = superFunction.isSuspend
            }.apply {
                overriddenSymbols += superFunction.symbol
                dispatchReceiverParameter = functionReferenceClass.thisReceiver?.copyTo(this)
            }

        private val IrFunction.originalName: Name
            get() = metadata?.name ?: name

        private fun JvmIrBuilder.generateSignature(target: IrFunctionSymbol): IrExpression =
            irCall(backendContext.ir.symbols.signatureStringIntrinsic).apply {
                putValueArgument(
                    0,
                    //don't pass receivers otherwise LocalDeclarationLowering will create additional captured parameters
                    IrFunctionReferenceImpl(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET, irFunctionReference.type, target,
                        irFunctionReference.typeArgumentsCount, target.owner.valueParameters.size,
                        irFunctionReference.reflectionTarget, null
                    ).apply {
                        copyTypeArgumentsFrom(irFunctionReference)
                    }
                )
            }

        private fun createLegacyMethodOverride(
            superFunction: IrSimpleFunction,
            generator: JvmIrBuilder.() -> IrExpression
        ): IrSimpleFunction =
            buildOverride(superFunction).apply {
                body = context.createJvmIrBuilder(symbol, startOffset, endOffset).run {
                    irExprBody(generator())
                }
            }

    }

    companion object {
        private fun IrBuilderWithScope.kClassReference(classType: IrType) =
            IrClassReferenceImpl(
                startOffset, endOffset, context.irBuiltIns.kClassClass.starProjectedType, context.irBuiltIns.kClassClass, classType
            )

        internal fun IrBuilderWithScope.kClassToJavaClass(kClassReference: IrExpression, context: JvmBackendContext) =
            irGet(context.ir.symbols.javaLangClass.starProjectedType, null, context.ir.symbols.kClassJava.owner.getter!!.symbol).apply {
                extensionReceiver = kClassReference
            }

        internal fun IrBuilderWithScope.javaClassReference(classType: IrType, context: JvmBackendContext) =
            kClassToJavaClass(kClassReference(classType), context)

        internal fun IrBuilderWithScope.calculateOwner(irContainer: IrDeclarationParent, context: JvmBackendContext): IrExpression {
            val kClass = calculateOwnerKClass(irContainer, context)

            if ((irContainer as? IrClass)?.isFileClass != true && irContainer !is IrPackageFragment)
                return kClass

            return irCall(context.ir.symbols.getOrCreateKotlinPackage).apply {
                putValueArgument(0, kClassToJavaClass(kClass, context))
                // Note that this name is not used in reflection. There should be the name of the referenced declaration's
                // module instead, but there's no nice API to obtain that name here yet
                // TODO: write the referenced declaration's module name and use it in reflection
                putValueArgument(1, irString(context.state.moduleName))
            }
        }

        internal fun IrBuilderWithScope.calculateOwnerKClass(irContainer: IrDeclarationParent, context: JvmBackendContext): IrExpression =
            kClassReference(getOwnerKClassType(irContainer, context))

        internal fun getOwnerKClassType(irContainer: IrDeclarationParent, context: JvmBackendContext): IrType =
            if (irContainer is IrClass) irContainer.defaultType
            else {
                // For built-in members (i.e. top level `toString`) we generate reference to an internal class for an owner.
                // This allows kotlin-reflect to understand that this is a built-in intrinsic which has no real declaration,
                // and construct a special KCallable object.
                context.ir.symbols.intrinsicsKotlinClass.defaultType
            }

        internal fun getCallableReferenceTopLevelFlag(declaration: IrDeclaration): Int =
            if (isCallableReferenceTopLevel(declaration)) 1 else 0

        internal fun isCallableReferenceTopLevel(declaration: IrDeclaration): Boolean =
            declaration.parent.let { it is IrClass && it.isFileClass }
    }
}
