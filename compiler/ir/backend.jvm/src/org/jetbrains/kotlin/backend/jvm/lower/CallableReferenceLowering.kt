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
import org.jetbrains.kotlin.backend.common.lower.insertCallsToDefaultArgumentStubs
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.codegen.isInlineFunctionCall
import org.jetbrains.kotlin.backend.jvm.codegen.isInlineIrExpression
import org.jetbrains.kotlin.backend.jvm.ir.isInlineParameter
import org.jetbrains.kotlin.backend.jvm.localDeclarationsPhase
import org.jetbrains.kotlin.builtins.functions.FunctionInvokeDescriptor
import org.jetbrains.kotlin.codegen.PropertyReferenceCodegen
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
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

    // Change calls to big arity invoke functions to vararg calls.
    override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
        markInlineFunctionReferences(expression)
        expression.transformChildrenVoid(this)

        if (expression.valueArgumentsCount < FunctionInvokeDescriptor.BIG_ARITY ||
            !expression.symbol.owner.parentAsClass.defaultType.isFunctionOrKFunction())
            return expression

        return IrCallImpl(
            expression.startOffset, expression.endOffset,
            expression.type, functionNInvokeFun.symbol, functionNInvokeFun.descriptor,
            1, expression.origin
        ).apply {
            putTypeArgument(0, expression.type)
            dispatchReceiver = expression.dispatchReceiver
            extensionReceiver = expression.extensionReceiver
            val vararg = IrVarargImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                context.ir.symbols.array.typeWith(context.irBuiltIns.anyNType),
                context.irBuiltIns.anyNType,
                (0 until expression.valueArgumentsCount).map { expression.getValueArgument(it)!! }
            )
            putValueArgument(0, vararg)
        }
    }

    private fun markInlineFunctionReferences(expression: IrFunctionAccessExpression) {
        val function = expression.symbol.owner
        if (!function.isInlineFunctionCall(context))
            return

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
        private val returnType = parameterTypes.last()

        private val useVararg
            get() = argumentTypes.size >= FunctionInvokeDescriptor.BIG_ARITY

        private val typeParameters = if (callee is IrConstructor)
            callee.parentAsClass.typeParameters + callee.typeParameters
        else
            callee.typeParameters
        private val typeArgumentsMap = typeParameters.associate { typeParam ->
            typeParam.symbol to irFunctionReference.getTypeArgument(typeParam.index)!!
        }

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
            createBridge(invokeMethod)

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

            body = context.createIrBuilder(symbol).run {
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

                            unboundIndex >= argumentTypes.size ->
                                // Unbound, but out of range - empty vararg or default value.
                                // TODO For suspend functions the last argument is continuation and it is implicit:
                                //      irCall(getContinuationSymbol, listOf(ourSymbol.descriptor.returnType!!))
                                null

                            // If a vararg parameter corresponds to exactly one KFunction argument, which is an array, that array
                            // is forwarded as a spread. In all other cases, excess arguments are packed into a new array.
                            //
                            //     fun f(x: (Int, Array<String>) -> String) = x(0, arrayOf("OK", "FAIL"))
                            //     fun g(x: (Int, String, String) -> String) = x(0, "OK", "FAIL")
                            //     fun h(i: Int, vararg xs: String) = xs[i]
                            //     f(::h) == g(::h)
                            //
                            parameter.isVararg && (unboundIndex < argumentTypes.size - 1 || argumentTypes.last() != parameter.type) ->
                                IrVarargImpl(
                                    startOffset, endOffset, parameter.type, parameter.varargElementType!!,
                                    (unboundIndex until argumentTypes.size).map { irGet(valueParameters[unboundIndex++]) }
                                )

                            else ->
                                irGet(valueParameters[unboundIndex++])
                        }?.let { putArgument(callee, parameter, it) }
                    }
                })
            }
        }

        // Build a bridge to the monomorphic invoke method. This is more elaborate than the usual BridgeLowering,
        // since we have special handling for functions with large arity (> 22 arguments). Large arity functions
        // are translated to functions with a single vararg argument, which checks the number of arguments and types
        // dynamically.
        private fun createBridge(invoke: IrSimpleFunction) {
            // Add supertypes
            val actualFunctionClass = if (useVararg)
                context.ir.symbols.functionN
            else
                context.ir.symbols.getJvmFunctionClass(argumentTypes.size)

            functionReferenceClass.superTypes += actualFunctionClass.typeWith(if (useVararg) listOf(returnType) else parameterTypes)
            val superFunction = actualFunctionClass.owner.functions.find { it.name.asString() == "invoke" }!!

            // Only add a bridge method when necessary
            if (context.state.typeMapper.mapAsmMethod(superFunction.descriptor) ==
                context.state.typeMapper.mapAsmMethod(invoke.descriptor)
            ) {
                invoke.overriddenSymbols += superFunction.symbol
                return
            }

            // Add the invoke bridge
            functionReferenceClass.addFunction {
                name = superFunction.name
                returnType = context.irBuiltIns.anyNType
                modality = Modality.FINAL
                visibility = Visibilities.PUBLIC
                origin = IrDeclarationOrigin.BRIDGE
            }.apply {
                overriddenSymbols += superFunction.symbol
                dispatchReceiverParameter = parentAsClass.thisReceiver!!.copyTo(this)
                if (useVararg)
                    valueParameters += superFunction.valueParameters[0].copyTo(this)
                else
                    superFunction.valueParameters.forEach { valueParameters += it.copyTo(this, type = context.irBuiltIns.anyNType) }

                body = context.createIrBuilder(symbol).irBlockBody(startOffset, endOffset) {
                    // Check the number of arguments for large arity functions
                    if (useVararg) {
                        +irIfThen(
                            irNotEquals(
                                irCall(arraySizeProperty.getter!!).apply {
                                    dispatchReceiver = irGet(valueParameters.single())
                                },
                                irInt(argumentTypes.size)
                            ),
                            irCall(context.irBuiltIns.illegalArgumentExceptionSymbol).apply {
                                putValueArgument(0, irString("Expected ${argumentTypes.size} arguments"))
                            }
                        )
                    }

                    +irReturn(irCall(invoke).apply {
                        dispatchReceiver = irGet(dispatchReceiverParameter!!)

                        for (parameter in invoke.valueParameters) {
                            val index = parameter.index

                            val argument = if (useVararg) {
                                val argArray = irGet(valueParameters.single())
                                val argIndex = irInt(index)
                                irCallOp(arrayGetFun.symbol, context.irBuiltIns.anyNType, argArray, argIndex)
                            } else {
                                irGet(valueParameters[index])
                            }

                            putValueArgument(index, irImplicitCast(argument, argumentTypes[index]))
                        }
                    })
                }
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
        internal fun IrBuilderWithScope.calculateOwner(irContainer: IrDeclarationParent, context: JvmBackendContext): IrExpression {
            val symbols = context.ir.symbols

            val isContainerPackage =
                (irContainer as? IrClass)?.origin == IrDeclarationOrigin.FILE_CLASS || irContainer is IrPackageFragment

            val clazzRef = IrClassReferenceImpl(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                symbols.javaLangClass.typeWith(),
                symbols.javaLangClass,
                // For built-in members (i.e. top level `toString`) we don't know any meaningful container, so we're generating Any.
                // The non-IR backend generates equally meaningless "kotlin/KotlinPackage" in this case (see KT-17151).
                (irContainer as? IrClass)?.defaultType ?: context.irBuiltIns.anyNType
            )

            if (!isContainerPackage) return clazzRef

            val jClass = irGet(symbols.javaLangClass.typeWith(), null, symbols.kClassJava.owner.getter!!.symbol).apply {
                extensionReceiver = clazzRef
            }
            return irCall(symbols.getOrCreateKotlinPackage).apply {
                putValueArgument(0, jClass)
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

    private val arraySizeProperty by lazy {
        context.irBuiltIns.arrayClass.owner.properties.single { it.name.toString() == "size" }
    }

    private val arrayGetFun by lazy {
        context.irBuiltIns.arrayClass.functionByName("get")
    }

    private val functionReferenceReceiverField =
        context.ir.symbols.functionReference.owner.declarations.single { it is IrField && it.name.toString() == "receiver" } as IrField

    private val functionGetSignature =
        context.ir.symbols.functionReference.functionByName("getSignature")

    private val functionGetName =
        context.ir.symbols.functionReference.functionByName("getName")

    private val functionGetOwner =
        context.ir.symbols.functionReference.functionByName("getOwner")

    private val functionNInvokeFun =
        context.ir.symbols.functionN.functionByName("invoke")
}
