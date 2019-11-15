/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.ir.isSuspend
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.ir.IrInlineReferenceLocator
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.irArray
import org.jetbrains.kotlin.backend.jvm.ir.isLambda
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

internal val callableReferencePhase = makeIrFilePhase(
    ::CallableReferenceLowering,
    name = "CallableReference",
    description = "Handle callable references"
)



// Originally copied from K/Native
internal class CallableReferenceLowering(private val context: JvmBackendContext) : FileLoweringPass, IrElementTransformerVoidWithContext() {
    // This pass ignores suspend function references and function references used in inline arguments to inline functions.
    private val ignoredFunctionReferences = mutableSetOf<IrCallableReference>()

    private val IrFunctionReference.isIgnored: Boolean
        get() = (!type.isFunctionOrKFunction() || ignoredFunctionReferences.contains(this)) && !isSuspendCallableReference()

    // TODO: Currently, origin of callable references is null. Do we need to create one?
    private fun IrFunctionReference.isSuspendCallableReference(): Boolean = isSuspend && origin == null

    override fun lower(irFile: IrFile) {
        ignoredFunctionReferences.addAll(IrInlineReferenceLocator.scan(context, irFile).inlineReferences)
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
        private val superType =
            samSuperType ?: (if (isLambda) context.ir.symbols.lambdaClass else context.ir.symbols.functionReference).defaultType

        private val functionReferenceClass = buildClass {
            setSourceRange(irFunctionReference)
            visibility = Visibilities.LOCAL
            // A callable reference results in a synthetic class, while a lambda is not synthetic.
            // We don't produce GENERATED_SAM_IMPLEMENTATION, which is always synthetic.
            origin = if (isLambda) JvmLoweredDeclarationOrigin.LAMBDA_IMPL else JvmLoweredDeclarationOrigin.FUNCTION_REFERENCE_IMPL
            name = SpecialNames.NO_NAME_PROVIDED
        }.apply {
            parent = currentDeclarationParent
            superTypes += superType
            if (samSuperType == null)
                superTypes += functionSuperClass.typeWith(parameterTypes)
            if (irFunctionReference.isSuspend) superTypes += context.ir.symbols.suspendFunctionInterface.defaultType
            createImplicitParameterDeclarationWithWrappedDescriptor()
            copyAttributes(irFunctionReference)
        }

        fun build(): IrExpression = context.createJvmIrBuilder(currentScope!!.scope.scopeOwnerSymbol).run {
            irBlock {
                val constructor = createConstructor()
                createInvokeMethod(
                    if (samSuperType != null && boundReceiver != null) {
                        irTemporary(boundReceiver.second)
                    } else null
                )

                if (!isLambda && samSuperType == null) {
                    createGetSignatureMethod(this@run.irSymbols.functionReferenceGetSignature.owner)
                    createGetNameMethod(this@run.irSymbols.functionReferenceGetName.owner)
                    createGetOwnerMethod(this@run.irSymbols.functionReferenceGetOwner.owner)
                }

                +functionReferenceClass
                +irCall(constructor.symbol).apply {
                    if (valueArgumentsCount > 0) putValueArgument(0, boundReceiver!!.second)
                }
            }
        }

        private fun createConstructor(): IrConstructor =
            functionReferenceClass.addConstructor {
                setSourceRange(irFunctionReference)
                origin = JvmLoweredDeclarationOrigin.GENERATED_MEMBER_IN_CALLABLE_REFERENCE
                returnType = functionReferenceClass.defaultType
                isPrimary = true
            }.apply {
                // Add receiver parameter for bound function references
                if (samSuperType == null) {
                    boundReceiver?.first?.let { param ->
                        valueParameters += param.copyTo(
                            irFunction = this,
                            index = 0,
                            type = param.type.substitute(typeArgumentsMap)
                        )
                    }
                }

                // Super constructor:
                // - For SAM references, the super class is Any
                // - For function references with bound receivers, accepts arity and receiver
                // - For lambdas and function references without bound receivers, accepts arity
                val constructor = if (samSuperType != null) {
                    context.irBuiltIns.anyClass.owner.constructors.single()
                } else {
                    superType.getClass()!!.constructors.single {
                        it.valueParameters.size == if (boundReceiver != null) 2 else 1
                    }
                }

                body = context.createIrBuilder(symbol).irBlockBody(startOffset, endOffset) {
                    +irDelegatingConstructorCall(constructor).apply {
                        if (samSuperType == null) {
                            putValueArgument(0, irInt(argumentTypes.size + if (irFunctionReference.isSuspend) 1 else 0))
                            if (boundReceiver != null)
                                putValueArgument(1, irGet(valueParameters.first()))
                        }
                    }
                    +IrInstanceInitializerCallImpl(startOffset, endOffset, functionReferenceClass.symbol, context.irBuiltIns.unitType)
                }
            }

        private fun createInvokeMethod(receiverVar: IrValueDeclaration?): IrSimpleFunction =
            functionReferenceClass.addFunction {
                name = superMethod.owner.name
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

            body = context.createIrBuilder(symbol).irBlockBody(startOffset, endOffset) {
                callee.body?.statements?.forEach { statement ->
                    +statement.transform(object : IrElementTransformerVoid() {
                        override fun visitGetValue(expression: IrGetValue): IrExpression {
                            val replacement = valueParameterMap[expression.symbol.owner]
                                ?: return super.visitGetValue(expression)

                            at(expression.startOffset, expression.endOffset)
                            return irGet(replacement)
                        }

                        override fun visitReturn(expression: IrReturn): IrExpression =
                            if (expression.returnTargetSymbol != callee.symbol) {
                                super.visitReturn(expression)
                            } else {
                                at(expression.startOffset, expression.endOffset)
                                irReturn(expression.value.transform(this, null))
                            }

                        override fun visitDeclaration(declaration: IrDeclaration): IrStatement {
                            if (declaration.parent == callee)
                                declaration.parent = this@createLambdaInvokeMethod
                            return super.visitDeclaration(declaration)
                        }
                    }, null)
                }
            }
        }

        private fun IrSimpleFunction.createFunctionReferenceInvokeMethod(receiver: IrValueDeclaration?) {
            for ((index, argumentType) in argumentTypes.withIndex()) {
                addValueParameter {
                    name = Name.identifier("p$index")
                    type = argumentType
                }
            }

            body = context.createJvmIrBuilder(symbol).run {
                var unboundIndex = 0
                irExprBody(irCall(callee).apply {
                    for ((typeParameter, typeArgument) in typeArgumentsMap) {
                        putTypeArgument(typeParameter.owner.index, typeArgument)
                    }

                    for (parameter in callee.explicitParameters) {
                        when {
                            boundReceiver?.first == parameter ->
                                // Bound receiver parameter. For function references, this is stored in a field of the superclass.
                                // For sam references, we just capture the value in a local variable and LocalDeclarationsLowering
                                // will put it into a field.
                                if (samSuperType == null)
                                    irImplicitCast(
                                        irGetField(irGet(dispatchReceiverParameter!!), irSymbols.functionReferenceReceiverField.owner),
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
            get() = (metadata as? MetadataSource.Function)?.descriptor?.name ?: name

        private fun createGetSignatureMethod(superFunction: IrSimpleFunction): IrSimpleFunction = buildOverride(superFunction).apply {
            body = context.createJvmIrBuilder(symbol, startOffset, endOffset).run {
                irExprBody(irCall(backendContext.ir.symbols.signatureStringIntrinsic).apply {
                    putValueArgument(0, irFunctionReference.deepCopyWithSymbols(symbol.owner))
                })
            }
        }

        private fun createGetNameMethod(superFunction: IrSimpleFunction): IrSimpleFunction = buildOverride(superFunction).apply {
            body = context.createIrBuilder(symbol, startOffset, endOffset).run {
                irExprBody(irString(callee.originalName.asString()))
            }
        }

        private fun createGetOwnerMethod(superFunction: IrSimpleFunction): IrSimpleFunction = buildOverride(superFunction).apply {
            body = context.createIrBuilder(symbol, startOffset, endOffset).run {
                irExprBody(calculateOwner(callee.parent, this@CallableReferenceLowering.context))
            }
        }
    }

    companion object {
        private fun IrBuilderWithScope.kClassReference(classType: IrType) =
            IrClassReferenceImpl(
                startOffset, endOffset, context.irBuiltIns.kClassClass.starProjectedType, context.irBuiltIns.kClassClass, classType
            )

        private fun IrBuilderWithScope.kClassToJavaClass(kClassReference: IrExpression, context: JvmBackendContext) =
            irGet(context.ir.symbols.javaLangClass.starProjectedType, null, context.ir.symbols.kClassJava.owner.getter!!.symbol).apply {
                extensionReceiver = kClassReference
            }

        internal fun IrBuilderWithScope.javaClassReference(classType: IrType, context: JvmBackendContext) =
            kClassToJavaClass(kClassReference(classType), context)

        internal fun IrBuilderWithScope.calculateOwner(irContainer: IrDeclarationParent, context: JvmBackendContext): IrExpression {
            // For built-in members (i.e. top level `toString`) we don't know any meaningful container, so we're generating Any.
            // The non-IR backend generates equally meaningless "kotlin/KotlinPackage" in this case (see KT-17151).
            val kClass = kClassReference((irContainer as? IrClass)?.defaultType ?: context.irBuiltIns.anyNType)

            if ((irContainer as? IrClass)?.origin != IrDeclarationOrigin.FILE_CLASS && irContainer !is IrPackageFragment)
                return kClass

            return irCall(context.ir.symbols.getOrCreateKotlinPackage).apply {
                putValueArgument(0, kClassToJavaClass(kClass, context))
                // Note that this name is not used in reflection. There should be the name of the referenced declaration's
                // module instead, but there's no nice API to obtain that name here yet
                // TODO: write the referenced declaration's module name and use it in reflection
                putValueArgument(1, irString(context.state.moduleName))
            }
        }
    }

    private val currentDeclarationParent
        get() = allScopes.last { it.irElement is IrDeclarationParent }.irElement as IrDeclarationParent
}
