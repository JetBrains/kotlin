/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.descriptors.isFunctionOrKFunctionType
import org.jetbrains.kotlin.backend.common.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.jvm.ir.isInlineParameter
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.codegen.isInlineFunctionCall
import org.jetbrains.kotlin.backend.jvm.codegen.isInlineIrExpression
import org.jetbrains.kotlin.builtins.functions.FunctionInvokeDescriptor
import org.jetbrains.kotlin.codegen.PropertyReferenceCodegen
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JavaVisibilities
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.org.objectweb.asm.Type

//Hack implementation to support CR java types in lower
class CrIrType(val type: Type) : IrType {
    override val annotations: List<IrConstructorCall> = emptyList()

    override fun equals(other: Any?): Boolean =
        other is CrIrType && type == other.type

    override fun hashCode(): Int =
        type.hashCode()
}

internal val callableReferencePhase = makeIrFilePhase(
    ::CallableReferenceLowering,
    name = "CallableReference",
    description = "Handle callable references"
)

//Originally was copied from K/Native
internal class CallableReferenceLowering(val context: JvmBackendContext) : FileLoweringPass {

    private var functionReferenceCount = 0

    private val inlineLambdaReferences = mutableSetOf<IrFunctionReference>()

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {

            override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
                val callee = expression.symbol.owner
                if (callee.isInlineFunctionCall(context)) {
                    //TODO: more wise filtering
                    for (valueParameter in callee.valueParameters) {
                        if (valueParameter.isInlineParameter()) {
                            expression.getValueArgument(valueParameter.index)?.let {
                                if (isInlineIrExpression(it)) {
                                    inlineLambdaReferences += (it as IrBlock).statements.filterIsInstance<IrFunctionReference>()
                                }
                            }
                        }
                    }
                }

                val argumentsCount = expression.valueArgumentsCount
                // Change calls to FunctionN with large N to varargs calls.
                val newCall = if (argumentsCount >= FunctionInvokeDescriptor.Factory.BIG_ARITY &&
                    callee.parentAsClass.defaultType.isFunctionOrKFunction()
                ) {
                    val vararg = IrVarargImpl(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        context.ir.symbols.array.typeWith(context.irBuiltIns.anyNType),
                        context.irBuiltIns.anyNType,
                        (0 until argumentsCount).map { i -> expression.getValueArgument(i)!! }
                    )
                    val invokeFun = context.ir.symbols.functionN.owner.declarations.single {
                        it is IrSimpleFunction && it.name.asString() == "invoke"
                    } as IrSimpleFunction

                    IrCallImpl(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        expression.type,
                        invokeFun.symbol, invokeFun.descriptor,
                        1,
                        expression.origin,
                        (expression as? IrCall)?.superQualifier?.let { context.ir.symbols.externalSymbolTable.referenceClass(it) }
                    ).apply {
                        putTypeArgument(0, expression.type)
                        dispatchReceiver = expression.dispatchReceiver
                        extensionReceiver = expression.extensionReceiver
                        putValueArgument(0, vararg)
                    }
                } else expression

                //TODO: clean
                return super.visitFunctionAccess(newCall)
            }

            override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
                expression.transformChildrenVoid(this)

                if (!expression.type.toKotlinType().isFunctionOrKFunctionType || inlineLambdaReferences.contains(expression)) {
                    // Not a subject of this lowering.
                    return expression
                }

                val currentDeclarationParent = allScopes.map { it.irElement }.last { it is IrDeclarationParent } as IrDeclarationParent
                val loweredFunctionReference = FunctionReferenceBuilder(currentDeclarationParent, expression).build()
                return context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol).irBlock(expression) {
                    +loweredFunctionReference.functionReferenceClass
                    +irCall(loweredFunctionReference.functionReferenceConstructor.symbol).apply {
                        expression.getArguments().forEachIndexed { index, argument ->
                            putValueArgument(index, argument.second)
                        }
                    }
                }
            }
        })
    }

    private val arrayGetFun by lazy {
        context.irBuiltIns.arrayClass.owner.functions.find { it.name.asString() == "get" }!!
    }

    private val arraySizeProperty by lazy {
        context.irBuiltIns.arrayClass.owner.properties.find { it.name.toString() == "size" }!!
    }

    private class BuiltFunctionReference(
        val functionReferenceClass: IrClass,
        val functionReferenceConstructor: IrConstructor
    )

    private inner class FunctionReferenceBuilder(
        val referenceParent: IrDeclarationParent,
        val irFunctionReference: IrFunctionReference
    ) {

        private val isLambda = irFunctionReference.origin == IrStatementOrigin.LAMBDA

        private val functionReferenceOrLambda = if (isLambda) context.ir.symbols.lambdaClass else context.ir.symbols.functionReference

        private val callee = irFunctionReference.symbol.owner
        private val calleeParameters = callee.explicitParameters
        private val boundCalleeParameters = irFunctionReference.getArgumentsWithIr().map { it.first }

        // The type of the reference is KFunction<in A1, ..., in An, out R>
        private val argumentTypes = (irFunctionReference.type as IrSimpleType).arguments.dropLast(1).map { (it as IrTypeProjection).type }
        private val returnType = ((irFunctionReference.type as IrSimpleType).arguments.last() as IrTypeProjection).type
        private val useVararg = (argumentTypes.size >= FunctionInvokeDescriptor.Factory.BIG_ARITY)

        private val typeParameters = if (callee is IrConstructor)
            callee.parentAsClass.typeParameters + callee.typeParameters
        else
            callee.typeParameters
        private val typeArgumentsMap = typeParameters.associate { typeParam ->
            typeParam.symbol to irFunctionReference.getTypeArgument(typeParam.index)!!
        }

        private val functionReferenceClass = buildClass {
            setSourceRange(irFunctionReference)
            origin = JvmLoweredDeclarationOrigin.FUNCTION_REFERENCE_IMPL
            name = "${callee.name.safeName()}\$${functionReferenceCount++}".synthesizedName
        }.apply {
            parent = referenceParent
            superTypes += functionReferenceOrLambda.owner.defaultType
            createImplicitParameterDeclarationWithWrappedDescriptor()
        }

        private val argumentToFieldMap = boundCalleeParameters.associate {
            it to buildField(it.name.safeName(), it.type)
        }

        fun build(): BuiltFunctionReference {
            val actualFunctionClass = if (useVararg)
                context.ir.symbols.functionN
            else
                context.ir.symbols.getJvmFunctionClass(argumentTypes.size)
            functionReferenceClass.superTypes +=
                actualFunctionClass.typeWith(if (useVararg) listOf(returnType) else argumentTypes + returnType)

            var suspendFunctionClass: IrClass? = null
            val lastParameterType = (calleeParameters - boundCalleeParameters).lastOrNull()?.type
            if (lastParameterType is IrSimpleType &&
                lastParameterType.classOrNull?.owner?.fqNameWhenAvailable?.asString() == "kotlin.coroutines.experimental.Continuation"
            ) {
                // If the last parameter is Continuation<> inherit from SuspendFunction.
                suspendFunctionClass = context.getTopLevelClass(FqName("kotlin.coroutines.SuspendFunction${argumentTypes.size - 1}")).owner
                val continuationType = (lastParameterType.arguments.single() as IrTypeProjection).type
                functionReferenceClass.superTypes += suspendFunctionClass.typeWith(argumentTypes + continuationType)
            }

            val constructor = createConstructor()
            createInvokeMethod(actualFunctionClass.owner.functions.find { it.name.asString() == "invoke" }!!)

            if (!isLambda) {
                createGetSignatureMethod(functionReferenceOrLambda.owner.functions.find { it.name.asString() == "getSignature" }!!)
                createGetNameMethod(functionReferenceOrLambda.owner.functions.find { it.name.asString() == "getName" }!!)
                createGetOwnerMethod(functionReferenceOrLambda.owner.functions.find { it.name.asString() == "getOwner" }!!)
                if (suspendFunctionClass != null) {
                    createInvokeMethod(suspendFunctionClass.functions.find { it.name.asString() == "invoke" }!!)
                }
            }

            return BuiltFunctionReference(functionReferenceClass, constructor)
        }

        private fun createConstructor(): IrConstructor =
            functionReferenceClass.addConstructor {
                setSourceRange(irFunctionReference)
                origin = JvmLoweredDeclarationOrigin.FUNCTION_REFERENCE_IMPL
                visibility = Visibilities.PUBLIC
                returnType = functionReferenceClass.defaultType
                isPrimary = true
            }.apply {
                for (param in boundCalleeParameters) {
                    valueParameters += param.copyTo(
                        this,
                        index = valueParameters.size,
                        type = param.type.substitute(typeArgumentsMap)
                    )
                }

                val kFunctionRefConstructor =
                    functionReferenceOrLambda.owner.constructors.single { it.valueParameters.size == if (isLambda) 1 else 2 }
                // The syntax (object::method) only allows to bind one of them.
                val hasReceiver = irFunctionReference.dispatchReceiver != null || irFunctionReference.extensionReceiver != null

                body = context.createIrBuilder(symbol).irBlockBody(startOffset, endOffset) {
                    +irDelegatingConstructorCall(kFunctionRefConstructor).apply {
                        putValueArgument(0, irInt(argumentTypes.size))
                        if (!isLambda) {
                            putValueArgument(1, if (hasReceiver) irGet(valueParameters[0]) else irNull())
                        }
                    }

                    // Save all arguments to fields.
                    // TODO don't write receiver again: use it from base class
                    boundCalleeParameters.forEachIndexed { index, it ->
                        +irSetField(
                            irGet(functionReferenceClass.thisReceiver!!),
                            argumentToFieldMap[it]!!,
                            irGet(valueParameters[index])
                        )
                    }
                    +IrInstanceInitializerCallImpl(startOffset, endOffset, functionReferenceClass.symbol, context.irBuiltIns.unitType)
                }
            }

        private fun createInvokeMethod(superFunction: IrSimpleFunction): IrSimpleFunction =
            buildOverride(superFunction, callee.returnType).apply {
                annotations.addAll(callee.annotations)

                if (useVararg) {
                    valueParameters.add(superFunction.valueParameters[0].copyTo(this))
                } else {
                    for ((parameter, type) in superFunction.valueParameters.zip(argumentTypes)) {
                        valueParameters += parameter.copyTo(this, type = type)
                    }
                }

                body = context.createIrBuilder(symbol).irBlockBody(startOffset, endOffset) {
                    if (useVararg) {
                        val varargParam = valueParameters.single()
                        +irIfThen(
                            irNotEquals(
                                irCall(arraySizeProperty.getter!!).apply {
                                    dispatchReceiver = irGet(varargParam)
                                },
                                irInt(argumentTypes.size)
                            ),
                            irCall(context.irBuiltIns.illegalArgumentExceptionSymbol).apply {
                                putValueArgument(0, irString("Expected ${argumentTypes.size} arguments"))
                            }
                        )
                    }

                    var unboundIndex = 0
                    fun consumeNextArgument() = if (useVararg) {
                        val type = argumentTypes[unboundIndex]
                        irBlock(resultType = type) {
                            val argArray = irGet(valueParameters.single())
                            val argIndex = irInt(unboundIndex++)
                            val argValue = irTemporary(irCallOp(arrayGetFun.symbol, context.irBuiltIns.anyNType, argArray, argIndex))
                            +irIfThen(
                                irNotIs(irGet(argValue), type),
                                irCall(context.irBuiltIns.illegalArgumentExceptionSymbol).apply {
                                    putValueArgument(0, irString("Wrong type, expected $type"))
                                }
                            )
                            +irImplicitCast(irGet(argValue), type)
                        }
                    } else {
                        irGet(valueParameters[unboundIndex++])
                    }

                    val delegation = irCall(irFunctionReference.symbol).apply {
                        for ((typeParameter, typeArgument) in typeArgumentsMap) {
                            putTypeArgument(typeParameter.owner.index, typeArgument)
                        }

                        for (parameter in calleeParameters) {
                            when {
                                argumentToFieldMap.contains(parameter) ->
                                    // Bound parameter - read from field.
                                    irGetField(irGet(dispatchReceiverParameter!!), argumentToFieldMap[parameter]!!)

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
                                        (unboundIndex until argumentTypes.size).map { consumeNextArgument() }
                                    )

                                else ->
                                    consumeNextArgument()
                            }?.let { putArgument(callee, parameter, it) }
                        }
                    }
                    +irReturn(delegation)
                }
            }

        private fun buildField(fieldName: Name, fieldType: IrType): IrField =
            functionReferenceClass.addField {
                setSourceRange(irFunctionReference)
                origin = JvmLoweredDeclarationOrigin.FUNCTION_REFERENCE_IMPL
                name = fieldName
                type = fieldType
                visibility = JavaVisibilities.PACKAGE_VISIBILITY
                isFinal = true
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
            val globalContext = context
            val state = globalContext.state
            val irContainer = callee.parent

            val isContainerPackage =
                ((irContainer as? IrClass)?.origin == IrDeclarationOrigin.FILE_CLASS) || irContainer is IrPackageFragment

            val type = when (irContainer) {
                // TODO: getDefaultType() here is wrong and won't work for arrays
                is IrClass -> state.typeMapper.mapType(irContainer.defaultType.toKotlinType())
                else -> state.typeMapper.mapOwner(callee.descriptor)
            }

            val clazz = globalContext.ir.symbols.javaLangClass
            val clazzRef = IrClassReferenceImpl(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                clazz.typeWith(),
                clazz,
                CrIrType(type)
            )

            body = context.createIrBuilder(symbol, startOffset, endOffset).run {
                irExprBody(if (isContainerPackage) {
                    irCall(globalContext.ir.symbols.getOrCreateKotlinPackage).apply {
                        putValueArgument(0, clazzRef)
                        // Note that this name is not used in reflection. There should be the name of the referenced declaration's
                        // module instead, but there's no nice API to obtain that name here yet
                        // TODO: write the referenced declaration's module name and use it in reflection
                        putValueArgument(1, irString(state.moduleName))
                    }
                } else {
                    irCall(globalContext.ir.symbols.getOrCreateKotlinClass).apply {
                        putValueArgument(0, clazzRef)
                    }
                })
            }
        }
    }

    //TODO rewrite
    private fun Name.safeName(): Name {
        return if (isSpecial) {
            val name = asString()
            Name.identifier("$${name.substring(1, name.length - 1)}")
        } else this
    }
}
