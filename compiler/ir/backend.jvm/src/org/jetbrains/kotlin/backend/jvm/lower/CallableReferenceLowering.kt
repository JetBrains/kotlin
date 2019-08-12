/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.codegen.isInlineFunctionCall
import org.jetbrains.kotlin.backend.jvm.codegen.isInlineIrExpression
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.irArray
import org.jetbrains.kotlin.backend.jvm.ir.isInlineParameter
import org.jetbrains.kotlin.codegen.PropertyReferenceCodegen
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
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JavaVisibilities
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addIfNotNull

internal val callableReferencePhase = makeIrFilePhase(
    ::CallableReferenceLowering,
    name = "CallableReference",
    description = "Handle callable references"
)

// Originally copied from K/Native
internal class CallableReferenceLowering(private val context: JvmBackendContext) : FileLoweringPass, IrElementTransformerVoidWithContext() {
    // This pass ignores function references used in inline arguments to inline functions references or in SAM conversions.
    // We also implicitly ignore all suspend function references by only dealing with subclasses of (K)Function and not
    // (K)SuspendFunction.
    private val ignoredFunctionReferences = mutableSetOf<IrFunctionReference>()

    private val IrFunctionReference.isIgnored: Boolean
        get() = !type.isFunctionOrKFunction() || ignoredFunctionReferences.contains(this)

    override fun lower(irFile: IrFile) = irFile.transformChildrenVoid(this)

    // Mark function references appearing as inlined arguments to inline functions.
    override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
        val function = expression.symbol.owner
        if (function.isInlineFunctionCall(context)) {
            for (parameter in function.valueParameters) {
                if (!parameter.isInlineParameter())
                    continue

                val valueArgument = expression.getValueArgument(parameter.index) ?: continue
                if (!isInlineIrExpression(valueArgument))
                    continue

                if (valueArgument is IrFunctionReference) {
                    ignoredFunctionReferences.add(valueArgument)
                } else if (valueArgument is IrBlock) {
                    ignoredFunctionReferences.addIfNotNull(valueArgument.statements.filterIsInstance<IrFunctionReference>().singleOrNull())
                }
            }
        }
        return super.visitFunctionAccess(expression)
    }

    // Ignore function references handled in SAM conversion
    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
        if (expression.operator == IrTypeOperator.SAM_CONVERSION) {
            val invokable = expression.argument
            if (invokable is IrFunctionReference) {
                ignoredFunctionReferences += invokable
            } else if (invokable is IrBlock && invokable.statements.last() is IrFunctionReference) {
                ignoredFunctionReferences += invokable.statements.last() as IrFunctionReference
            }
        }
        return super.visitTypeOperator(expression)
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

    private inner class FunctionReferenceBuilder(val irFunctionReference: IrFunctionReference) {
        private val isLambda = irFunctionReference.origin.isLambda

        private val functionReferenceOrLambda = if (isLambda) context.ir.symbols.lambdaClass else context.ir.symbols.functionReference

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

        private val functionReferenceClass = buildClass {
            setSourceRange(irFunctionReference)
            visibility = Visibilities.LOCAL
            // A callable reference results in a synthetic class, while a lambda is not synthetic.
            origin = if (isLambda) JvmLoweredDeclarationOrigin.LAMBDA_IMPL else JvmLoweredDeclarationOrigin.FUNCTION_REFERENCE_IMPL
            name = Name.special("<function reference to ${callee.fqNameWhenAvailable}>")
        }.apply {
            parent = currentDeclarationParent
            superTypes += functionReferenceOrLambda.owner.defaultType
            createImplicitParameterDeclarationWithWrappedDescriptor()
            copyAttributes(irFunctionReference)
        }

        fun build(): IrExpression {
            val constructor = createConstructor()
            val invokeMethod = createInvokeMethod()

            val functionClass = context.ir.symbols.getJvmFunctionClass(argumentTypes.size)
            functionReferenceClass.superTypes += functionClass.typeWith(parameterTypes)
            invokeMethod.overriddenSymbols += functionClass.functions.single { it.owner.name.asString() == "invoke" }

            if (!isLambda) {
                createGetSignatureMethod(functionGetSignature)
                createGetNameMethod(functionGetName)
                createGetOwnerMethod(functionGetOwner)
            }

            return context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol).irBlock(irFunctionReference) {
                +functionReferenceClass
                +irCall(constructor.symbol).apply {
                    boundReceiver?.second?.let { putValueArgument(0, it) }
                }
            }
        }

        private fun createConstructor(): IrConstructor =
            functionReferenceClass.addConstructor {
                setSourceRange(irFunctionReference)
                origin = JvmLoweredDeclarationOrigin.FUNCTION_REFERENCE_IMPL
                visibility = if (inInlineFunctionScope) Visibilities.PUBLIC else JavaVisibilities.PACKAGE_VISIBILITY
                returnType = functionReferenceClass.defaultType
                isPrimary = true
            }.apply {
                // Add receiver parameter for bound function references
                boundReceiver?.first?.let { param ->
                    valueParameters += param.copyTo(
                        irFunction = this,
                        index = valueParameters.size,
                        type = param.type.substitute(typeArgumentsMap)
                    )
                }

                // Super constructor:
                // - For function references with bound receivers, accepts arity and receiver
                // - For lambdas and function references without bound receivers, accepts arity
                val kFunctionRefConstructor = functionReferenceOrLambda.owner.constructors.single {
                    it.valueParameters.size == if (boundReceiver != null) 2 else 1
                }

                body = context.createIrBuilder(symbol).irBlockBody(startOffset, endOffset) {
                    +irDelegatingConstructorCall(kFunctionRefConstructor).apply {
                        putValueArgument(0, irInt(argumentTypes.size))
                        if (boundReceiver != null)
                            putValueArgument(1, irGet(valueParameters.first()))
                    }
                    +IrInstanceInitializerCallImpl(startOffset, endOffset, functionReferenceClass.symbol, context.irBuiltIns.unitType)
                }
            }

        private fun createInvokeMethod(): IrSimpleFunction =
            functionReferenceClass.addFunction {
                name = Name.identifier("invoke")
                returnType = callee.returnType
                isSuspend = callee.isSuspend
            }.apply {
                dispatchReceiverParameter = parentAsClass.thisReceiver!!.copyTo(this)
                if (isLambda) createLambdaInvokeMethod() else createFunctionReferenceInvokeMethod()
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

        private fun IrSimpleFunction.createFunctionReferenceInvokeMethod() {
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
                                // Bound receiver parameter
                                irImplicitCast(
                                    irGetField(irGet(dispatchReceiverParameter!!), functionReferenceReceiverField),
                                    boundReceiver.second.type
                                )

                            // If a vararg parameter corresponds to exactly one KFunction argument, which is an array, that array
                            // is forwarded as is. In all other cases, excess arguments are packed into a new array.
                            //
                            //     fun f(x: (Int, Array<String>) -> String) = x(0, arrayOf("OK", "FAIL"))
                            //     fun g(x: (Int, String, String) -> String) = x(0, "OK", "FAIL")
                            //     fun h(i: Int, vararg xs: String) = xs[i]
                            //     f(::h) == g(::h)
                            //
                            parameter.isVararg && unboundIndex < argumentTypes.size && parameter.type == valueParameters[unboundIndex].type ->
                                irArray(parameter.type) { addSpread(irGet(valueParameters[unboundIndex++])) }
                            parameter.isVararg && (unboundIndex < argumentTypes.size || !parameter.hasDefaultValue()) ->
                                irArray(parameter.type) {
                                    (unboundIndex until argumentTypes.size).forEach { +irGet(valueParameters[unboundIndex++]) }
                                }

                            unboundIndex >= argumentTypes.size ->
                                // Default value argument
                                // TODO For suspend functions the last argument is continuation and it is implicit:
                                //      irCall(getContinuationSymbol, listOf(ourSymbol.descriptor.returnType!!))
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
                origin = functionReferenceClass.origin
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
            val state = context.state
            body = context.createIrBuilder(symbol, startOffset, endOffset).run {
                // TODO do not use descriptors
                irExprBody(irString(PropertyReferenceCodegen.getSignatureString(irFunctionReference.symbol.descriptor, state)))
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
                startOffset,
                endOffset,
                context.irBuiltIns.kClassClass.typeWith(),
                context.irBuiltIns.kClassClass,
                classType
            )

        private fun IrBuilderWithScope.kClassToJavaClass(kClassReference: IrExpression, context: JvmBackendContext) =
            irGet(context.ir.symbols.javaLangClass.typeWith(), null, context.ir.symbols.kClassJava.owner.getter!!.symbol).apply {
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

    private val IrStatementOrigin?.isLambda
        get() = this == IrStatementOrigin.LAMBDA || this == IrStatementOrigin.ANONYMOUS_FUNCTION

    private val currentDeclarationParent
        get() = allScopes.last { it.irElement is IrDeclarationParent }.irElement as IrDeclarationParent

    private val inInlineFunctionScope: Boolean
        get() = allScopes.any { scope -> (scope.irElement as? IrFunction)?.isInline ?: false }

    private fun IrClassSymbol.functionByName(name: String) = owner.functions.single { it.name.asString() == name }

    private val functionReferenceReceiverField =
        context.ir.symbols.functionReference.owner.declarations.single { it is IrField && it.name.toString() == "receiver" } as IrField

    private val functionGetSignature =
        context.ir.symbols.functionReference.functionByName("getSignature")

    private val functionGetName =
        context.ir.symbols.functionReference.functionByName("getName")

    private val functionGetOwner =
        context.ir.symbols.functionReference.functionByName("getOwner")
}
