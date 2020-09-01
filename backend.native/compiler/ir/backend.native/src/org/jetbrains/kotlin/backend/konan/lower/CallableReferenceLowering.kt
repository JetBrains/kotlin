/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ir.*
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.konan.llvm.functionName
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedClassConstructorDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedClassDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal class CallableReferenceLowering(val context: Context): FileLoweringPass {

    private object DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL : IrDeclarationOriginImpl("FUNCTION_REFERENCE_IMPL")

    companion object {
        fun isLoweredCallableReference(declaration: IrDeclaration): Boolean =
                declaration.origin == DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL
    }

    private val kTypeGenerator = KTypeGenerator(context)

    override fun lower(irFile: IrFile) {
        var generatedClasses = mutableListOf<IrClass>()
        irFile.transform(object: IrElementTransformerVoidWithContext() {

            private val stack = mutableListOf<IrElement>()

            override fun visitElement(element: IrElement): IrElement {
                stack.push(element)
                val result = super.visitElement(element)
                stack.pop()
                return result
            }

            override fun visitExpression(expression: IrExpression): IrExpression {
                stack.push(expression)
                val result = super.visitExpression(expression)
                stack.pop()
                return result
            }

            override fun visitDeclaration(declaration: IrDeclarationBase): IrStatement {
                lateinit var tempGeneratedClasses: MutableList<IrClass>
                if (declaration is IrClass) {
                    tempGeneratedClasses = generatedClasses
                    generatedClasses = mutableListOf()
                }
                stack.push(declaration)
                val result = super.visitDeclaration(declaration)
                stack.pop()
                if (declaration is IrClass) {
                    declaration.declarations += generatedClasses
                    generatedClasses = tempGeneratedClasses
                }
                return result
            }

            override fun visitSpreadElement(spread: IrSpreadElement): IrSpreadElement {
                stack.push(spread)
                val result = super.visitSpreadElement(spread)
                stack.pop()
                return result
            }

            override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
                expression.transformChildrenVoid(this)

                for (i in stack.size - 1 downTo 0) {
                    val cur = stack[i]
                    if (cur is IrBlock)
                        continue
                    if (cur !is IrCall)
                        break
                    val argument = if (i < stack.size - 1) stack[i + 1] else expression
                    val parameter = cur.symbol.owner.valueParameters.singleOrNull {
                        cur.getValueArgument(it.index) == argument
                    }
                    if (parameter?.annotations?.findAnnotation(VOLATILE_LAMBDA_FQ_NAME) != null) {
                        return expression
                    }
                    break
                }

                if (!expression.type.isFunction() && !expression.type.isKFunction()
                        && !expression.type.isKSuspendFunction()) {
                    // Not a subject of this lowering.
                    return expression
                }

                val parent: IrDeclarationContainer = (currentClass?.irElement as? IrClass) ?: irFile
                val loweredFunctionReference = FunctionReferenceBuilder(parent, expression).build()
                generatedClasses.add(loweredFunctionReference.functionReferenceClass)
                val irBuilder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol,
                        expression.startOffset, expression.endOffset)
                return irBuilder.irCall(loweredFunctionReference.functionReferenceConstructor.symbol).apply {
                    expression.getArguments().forEachIndexed { index, argument ->
                        putValueArgument(index, argument.second)
                    }
                }
            }
        }, null)
        irFile.declarations += generatedClasses
    }

    private class BuiltFunctionReference(val functionReferenceClass: IrClass,
                                         val functionReferenceConstructor: IrConstructor)

    private val VOLATILE_LAMBDA_FQ_NAME = FqName.fromSegments(listOf("kotlin", "native", "internal", "VolatileLambda"))

    private val symbols = context.ir.symbols
    private val irBuiltIns = context.irBuiltIns

    private val getContinuationSymbol = symbols.getContinuation
    private val continuationClassSymbol = getContinuationSymbol.owner.returnType.classifierOrFail as IrClassSymbol

    private inner class FunctionReferenceBuilder(val parent: IrDeclarationParent,
                                                 val functionReference: IrFunctionReference) {

        private val startOffset = functionReference.startOffset
        private val endOffset = functionReference.endOffset
        private val referencedFunction = functionReference.symbol.owner
        private val functionParameters = referencedFunction.explicitParameters
        private val boundFunctionParameters = functionReference.getArgumentsWithIr().map { it.first }
        private val unboundFunctionParameters = functionParameters - boundFunctionParameters

        private val typeArgumentsMap = referencedFunction.typeParameters.associate { typeParam ->
            typeParam.symbol to functionReference.getTypeArgument(typeParam.index)!!
        }

        private val functionReferenceClass: IrClass = WrappedClassDescriptor().let {
            IrClassImpl(
                    startOffset,endOffset,
                    DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL,
                    IrClassSymbolImpl(it),
                    "${referencedFunction.name}\$FUNCTION_REFERENCE\$${context.functionReferenceCount++}".synthesizedName,
                    ClassKind.CLASS,
                    DescriptorVisibilities.PRIVATE,
                    Modality.FINAL,
                    isCompanion = false,
                    isInner = false,
                    isData = false,
                    isExternal = false,
                    isInline = false,
                    isExpect = false,
                    isFun = false
            ).apply {
                it.bind(this)
                parent = this@FunctionReferenceBuilder.parent
                createParameterDeclarations()
            }
        }

        private val functionReferenceThis = functionReferenceClass.thisReceiver!!

        private val argumentToPropertiesMap = boundFunctionParameters.associate {
            it to createField(
                    startOffset, endOffset,
                    DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL,
                    it.type,
                    it.name,
                    isMutable = false,
                    owner = functionReferenceClass
            )
        }

        private fun IrClass.getInvokeFunction() = simpleFunctions().single { it.name.asString() == "invoke" }

        private val kFunctionImplSymbol = symbols.kFunctionImpl
        private val kFunctionImplConstructorSymbol = kFunctionImplSymbol.constructors.single()
        private val kSuspendFunctionImplSymbol = symbols.kSuspendFunctionImpl
        private val kSuspendFunctionImplConstructorSymbol = kSuspendFunctionImplSymbol.constructors.single()

        val isKFunction = functionReference.type.isKFunction()
        val isKSuspendFunction = functionReference.type.isKSuspendFunction()

        fun build(): BuiltFunctionReference {
            val numberOfParameters = unboundFunctionParameters.size
            val functionParameterTypes = unboundFunctionParameters.map { it.type }
            val superTypes = mutableListOf<IrType>()
            val functionClass: IrClass
            val suspendFunctionClass: IrClass?
            if (isKSuspendFunction) {
                superTypes += kSuspendFunctionImplSymbol.typeWith(referencedFunction.returnType)
                functionClass = symbols.functionN(numberOfParameters + 1).owner
                val continuationType = continuationClassSymbol.typeWith(referencedFunction.returnType)
                superTypes += functionClass.typeWith(functionParameterTypes + continuationType + irBuiltIns.anyNType)
                suspendFunctionClass = symbols.kSuspendFunctionN(numberOfParameters).owner
                superTypes += suspendFunctionClass.typeWith(functionParameterTypes + referencedFunction.returnType)
            }
            else {
                superTypes += if (isKFunction)
                    kFunctionImplSymbol.typeWith(referencedFunction.returnType)
                else
                    irBuiltIns.anyType
                functionClass = (if (isKFunction) symbols.kFunctionN(numberOfParameters) else symbols.functionN(numberOfParameters)).owner
                superTypes += functionClass.typeWith(functionParameterTypes + referencedFunction.returnType)
                val lastParameterType = unboundFunctionParameters.lastOrNull()?.type
                if (lastParameterType?.classifierOrNull != continuationClassSymbol)
                    suspendFunctionClass = null
                else {
                    lastParameterType as IrSimpleType
                    // If the last parameter is Continuation<> inherit from SuspendFunction.
                    suspendFunctionClass = symbols.suspendFunctionN(numberOfParameters - 1).owner
                    val suspendFunctionClassTypeParameters = functionParameterTypes.dropLast(1) +
                            (lastParameterType.arguments.single().typeOrNull ?: irBuiltIns.anyNType)
                    superTypes += suspendFunctionClass.symbol.typeWith(suspendFunctionClassTypeParameters)
                }
            }

            val constructor = buildConstructor()
            if (!isKSuspendFunction)
                buildInvokeMethod(functionClass.getInvokeFunction())
            suspendFunctionClass?.let {
                val invokeMethod = buildInvokeMethod(it.getInvokeFunction())
                if (isKSuspendFunction)
                    invokeMethod.overriddenSymbols += functionClass.getInvokeFunction().symbol
            }

            functionReferenceClass.superTypes += superTypes
            functionReferenceClass.addFakeOverridesViaIncorrectHeuristic()

            return BuiltFunctionReference(functionReferenceClass, constructor)
        }

        private fun buildConstructor(): IrConstructor = WrappedClassConstructorDescriptor().let {
            IrConstructorImpl(
                    startOffset, endOffset,
                    DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL,
                    IrConstructorSymbolImpl(it),
                    Name.special("<init>"),
                    DescriptorVisibilities.PUBLIC,
                    functionReferenceClass.defaultType,
                    isInline = false,
                    isExternal = false,
                    isPrimary = true,
                    isExpect = false
            ).apply {
                it.bind(this)
                parent = functionReferenceClass
                functionReferenceClass.declarations += this

                valueParameters += boundFunctionParameters.mapIndexed { index, parameter ->
                    parameter.copyTo(this, DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL, index,
                            type = parameter.type.substitute(typeArgumentsMap))
                }

                body = context.createIrBuilder(symbol, startOffset, endOffset).irBlockBody {
                    val superConstructor = when {
                        isKFunction -> kFunctionImplConstructorSymbol.owner
                        isKSuspendFunction -> kSuspendFunctionImplConstructorSymbol.owner
                        else -> irBuiltIns.anyClass.owner.constructors.single()
                    }
                    +irDelegatingConstructorCall(superConstructor).apply applyIrDelegationConstructorCall@ {
                        if (!isKFunction && !isKSuspendFunction) return@applyIrDelegationConstructorCall
                        val name = ((referencedFunction as? IrSimpleFunction)?.attributeOwnerId as? IrSimpleFunction)?.name
                                ?: referencedFunction.name
                        putValueArgument(0, irString(name.asString()))
                        putValueArgument(1, irString((functionReference.symbol.owner).fullName))
                        putValueArgument(2, irBoolean(boundFunctionParameters.isNotEmpty()))
                        val needReceiver = boundFunctionParameters.singleOrNull()?.descriptor is ReceiverParameterDescriptor
                        val receiver = if (needReceiver) irGet(valueParameters.single()) else irNull()
                        putValueArgument(3, receiver)
                        putValueArgument(4, with(kTypeGenerator) { irKType(referencedFunction.returnType) })
                    }
                    +IrInstanceInitializerCallImpl(startOffset, endOffset, functionReferenceClass.symbol, irBuiltIns.unitType)
                    // Save all arguments to fields.
                    boundFunctionParameters.forEachIndexed { index, parameter ->
                        +irSetField(irGet(functionReferenceThis), argumentToPropertiesMap[parameter]!!, irGet(valueParameters[index]))
                    }
                }
            }
        }

        private val IrFunction.fullName: String
            get() = parent.fqNameForIrSerialization.child(Name.identifier(functionName)).asString()

        private fun buildInvokeMethod(superFunction: IrSimpleFunction): IrSimpleFunction = WrappedSimpleFunctionDescriptor().let {
            IrFunctionImpl(
                    startOffset, endOffset,
                    DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL,
                    IrSimpleFunctionSymbolImpl(it),
                    superFunction.name,
                    DescriptorVisibilities.PRIVATE,
                    Modality.FINAL,
                    referencedFunction.returnType,
                    isInline = false,
                    isExternal = false,
                    isTailrec = false,
                    isSuspend = superFunction.isSuspend,
                    isExpect = false,
                    isFakeOverride = false,
                    isOperator = false,
                    isInfix = false
            ).apply {
                it.bind(this)
                val function = this
                parent = functionReferenceClass
                functionReferenceClass.declarations += function

                this.createDispatchReceiverParameter()

                valueParameters += superFunction.valueParameters.mapIndexed { index, parameter ->
                    parameter.copyTo(function, DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL, index,
                            type = parameter.type.substitute(typeArgumentsMap))
                }

                overriddenSymbols += superFunction.symbol

                body = context.createIrBuilder(function.symbol, startOffset, endOffset).irBlockBody(startOffset, endOffset) {
                    +irReturn(
                            irCall(functionReference.symbol).apply {
                                var unboundIndex = 0
                                val unboundArgsSet = unboundFunctionParameters.toSet()
                                for (parameter in functionParameters) {
                                    val argument =
                                            if (!unboundArgsSet.contains(parameter))
                                            // Bound parameter - read from field.
                                                irGetField(
                                                        irGet(function.dispatchReceiverParameter!!),
                                                        argumentToPropertiesMap[parameter]!!
                                                )
                                            else {
                                                if (function.isSuspend && unboundIndex == valueParameters.size)
                                                // For suspend functions the last argument is continuation and it is implicit.
                                                    irCall(getContinuationSymbol.owner, listOf(returnType))
                                                else
                                                    irGet(valueParameters[unboundIndex++])
                                            }
                                    when (parameter) {
                                        referencedFunction.dispatchReceiverParameter -> dispatchReceiver = argument
                                        referencedFunction.extensionReceiverParameter -> extensionReceiver = argument
                                        else -> putValueArgument(parameter.index, argument)
                                    }
                                }
                                assert(unboundIndex == valueParameters.size) { "Not all arguments of <invoke> are used" }
                            }
                    )
                }
            }
        }
    }
}
