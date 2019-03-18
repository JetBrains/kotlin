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
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.codegen.isInlineIrExpression
import org.jetbrains.kotlin.codegen.PropertyReferenceCodegen
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.declarations.buildConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JavaVisibilities
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.org.objectweb.asm.Type

//Hack implementation to support CR java types in lower
class CrIrType(val type: Type) : IrType {
    override val annotations = emptyList()
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

            override fun visitCall(expression: IrCall): IrExpression {
                val callee = expression.symbol.owner
                if (callee.isInlineFunction(context)) {
                    //TODO: more wise filtering
                    callee.valueParameters.forEach { valueParameter ->
                        if (valueParameter.isInlineParameter()) {
                            expression.getValueArgument(valueParameter.index)?.let {
                                if (isInlineIrExpression(it)) {
                                    (it as IrBlock).statements.filterIsInstance<IrFunctionReference>().forEach { reference ->
                                        inlineLambdaReferences.add(reference)
                                    }
                                }
                            }
                        }
                    }
                }

                val argumentsCount = expression.valueArgumentsCount
                // Change calls to FunctionN with large N to varargs calls.
                val newCall = if (argumentsCount > MAX_ARGCOUNT_WITHOUT_VARARG &&
                    callee.parentAsClass.defaultType.isFunctionOrKFunction()
                ) {
                    val vararg = IrVarargImpl(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        context.ir.symbols.array.typeWith(),
                        context.irBuiltIns.anyClass.typeWith(),
                        (0 until argumentsCount).map { i -> expression.getValueArgument(i)!! }
                    )
                    val invokeFun = context.getIrClass(FqName("kotlin.jvm.functions.FunctionN")).owner.declarations.single {
                        it is IrSimpleFunction && it.name.asString() == "invoke"
                    } as IrSimpleFunction

                    IrCallImpl(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        expression.type,
                        invokeFun.symbol, invokeFun.descriptor,
                        1,
                        expression.origin,
                        expression.superQualifier?.let { context.ir.symbols.externalSymbolTable.referenceClass(it) }
                    ).apply {
                        putTypeArgument(0, expression.type)
                        dispatchReceiver = expression.dispatchReceiver
                        extensionReceiver = expression.extensionReceiver
                        putValueArgument(0, vararg)
                    }
                } else expression

                //TODO: clean
                return super.visitCall(newCall)
            }

            override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
                expression.transformChildrenVoid(this)

                if (!expression.type.toKotlinType().isFunctionOrKFunctionType || inlineLambdaReferences.contains(expression)) {
                    // Not a subject of this lowering.
                    return expression
                }

                val currentDeclarationParent = allScopes.map { it.irElement }.last { it is IrDeclarationParent } as IrDeclarationParent
                val loweredFunctionReference = FunctionReferenceBuilder(currentDeclarationParent, expression).build()
                val irBuilder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol, expression.startOffset, expression.endOffset)
                return irBuilder.irBlock(expression) {
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

    private class BuiltFunctionReference(
        val functionReferenceClass: IrClass,
        val functionReferenceConstructor: IrConstructor
    )

    private val continuationClass = context.getIrClass(FqName("kotlin.coroutines.experimental.Continuation")).owner

    //private val getContinuationSymbol = context.ir.symbols.getContinuation

    private inner class FunctionReferenceBuilder(
        val referenceParent: IrDeclarationParent,
        val irFunctionReference: IrFunctionReference
    ) {

        private val callee = irFunctionReference.symbol.owner
        private val calleeParameters = callee.explicitParameters
        private val boundCalleeParameters = irFunctionReference.getArgumentsWithIr().map { it.first }
        private val unboundCalleeParameters = calleeParameters - boundCalleeParameters

        private val typeArgumentsMap = callee.typeParameters.associate { typeParam ->
            typeParam to irFunctionReference.getTypeArgument(typeParam.index)!!
        }


        private lateinit var functionReferenceClass: IrClass
        private lateinit var functionReferenceThis: IrValueParameterSymbol
        private lateinit var argumentToFieldMap: Map<IrValueParameter, IrField>

        private val isLambda = irFunctionReference.origin == IrStatementOrigin.LAMBDA

        private val functionReferenceOrLambda = if (isLambda) context.ir.symbols.lambdaClass else context.ir.symbols.functionReference

        var useVararg: Boolean = false

        fun build(): BuiltFunctionReference {
            val returnType = irFunctionReference.symbol.owner.returnType
            val functionReferenceClassSuperTypes: MutableList<IrType> = mutableListOf(
                functionReferenceOrLambda.owner.defaultType // type arguments?
            )

            val numberOfParameters = unboundCalleeParameters.size
            useVararg = (numberOfParameters > MAX_ARGCOUNT_WITHOUT_VARARG)

            val functionClassSymbol = if (useVararg)
                context.getIrClass(FqName("kotlin.jvm.functions.FunctionN"))
            else
                context.getIrClass(FqName("kotlin.jvm.functions.Function$numberOfParameters"))
            val functionClass = functionClassSymbol.owner
            val functionParameterTypes = unboundCalleeParameters.map { it.type }
            val functionClassTypeParameters = if (useVararg)
                listOf(returnType)
            else
                functionParameterTypes + returnType
            functionReferenceClassSuperTypes += IrSimpleTypeImpl(
                functionClassSymbol,
                hasQuestionMark = false,
                arguments = functionClassTypeParameters.map { makeTypeProjection(it, Variance.INVARIANT) },
                annotations = emptyList()
            )

            var suspendFunctionClass: IrClass? = null
            val lastParameterType = unboundCalleeParameters.lastOrNull()?.type
            if ((lastParameterType as? IrSimpleType)?.classifier == continuationClass) {
                // If the last parameter is Continuation<> inherit from SuspendFunction.
                suspendFunctionClass = context.getIrClass(FqName("kotlin.Suspendfunction${numberOfParameters - 1}")).owner
                val suspendFunctionClassTypeParameters = functionParameterTypes.dropLast(1) +
                        (lastParameterType.arguments.single() as IrTypeProjection).type
                functionReferenceClassSuperTypes += IrSimpleTypeImpl(
                    suspendFunctionClass.symbol,
                    hasQuestionMark = false,
                    arguments = suspendFunctionClassTypeParameters.map { makeTypeProjection(it, Variance.INVARIANT) },
                    annotations = emptyList()
                )
            }

            functionReferenceClass = buildClass {
                setSourceRange(irFunctionReference)
                origin = JvmLoweredDeclarationOrigin.FUNCTION_REFERENCE_IMPL
                name = "${callee.name}\$${functionReferenceCount++}".synthesizedName
                kind = ClassKind.CLASS
                visibility = Visibilities.PUBLIC
                modality = Modality.FINAL
            }.apply {
                parent = referenceParent
                superTypes.addAll(functionReferenceClassSuperTypes)
                createImplicitParameterDeclarationWithWrappedDescriptor()
            }

            functionReferenceThis = functionReferenceClass.thisReceiver!!.symbol

            argumentToFieldMap = boundCalleeParameters.associate {
                it to buildField(it.name.safeName(), it.type)
            }

            val constructor = createConstructor()
            functionReferenceClass.declarations.add(constructor)

            val superInvokeFunction = functionClass.functions.find { it.name.asString() == "invoke" }!!
            val invokeMethod = createInvokeMethod(superInvokeFunction)
            functionReferenceClass.declarations.add(invokeMethod)

            if (!isLambda) {
                val getSignatureMethod =
                    createGetSignatureMethod(functionReferenceOrLambda.owner.functions.find { it.name.asString() == "getSignature"}!!)
                val getNameMethod =
                    createGetNameMethod(functionReferenceOrLambda.owner.properties.find { it.name.asString() == "name" }!!)
                val getOwnerMethod =
                    createGetOwnerMethod(functionReferenceOrLambda.owner.functions.find { it.name.asString() == "getOwner" }!!)

                val suspendInvokeMethod =
                    if (suspendFunctionClass != null) {
                        val suspendInvokeFunction =
                            suspendFunctionClass.functions.find { it.name.asString() == "invoke" }!!
                        createInvokeMethod(suspendInvokeFunction)
                    } else null

                functionReferenceClass.declarations.add(getSignatureMethod)
                functionReferenceClass.declarations.add(getNameMethod)
                functionReferenceClass.declarations.add(getOwnerMethod)
                suspendInvokeMethod?.let { functionReferenceClass.declarations.add(it) }
            }

            return BuiltFunctionReference(functionReferenceClass, constructor)
        }

        private fun createConstructor(): IrConstructor =
            buildConstructor {
                setSourceRange(irFunctionReference)
                origin = JvmLoweredDeclarationOrigin.FUNCTION_REFERENCE_IMPL
                visibility = Visibilities.PUBLIC
                returnType = functionReferenceClass.defaultType
                isPrimary = true
            }.apply {
                val constructor = this
                parent = functionReferenceClass

                val boundArgsSet = boundCalleeParameters.toSet()
                for (param in callee.explicitParameters) {
                    if (param in boundArgsSet) {
                        val newParam = param.copyTo(
                            constructor,
                            index = valueParameters.size,
                            type = param.type.substitute(typeArgumentsMap)
                        )
                        valueParameters.add(newParam)
                    }
                }

                val kFunctionRefConstructorSymbol =
                    functionReferenceOrLambda.constructors.filter { it.owner.valueParameters.size == if (isLambda) 1 else 2 }.single()

                val irBuilder = context.createIrBuilder(this.symbol, startOffset, endOffset)
                body = irBuilder.irBlockBody {
                    +IrDelegatingConstructorCallImpl(
                        startOffset, endOffset, context.irBuiltIns.unitType,
                        kFunctionRefConstructorSymbol, kFunctionRefConstructorSymbol.descriptor
                    ).apply {
                        val const =
                            IrConstImpl.int(startOffset, endOffset, context.irBuiltIns.intType, unboundCalleeParameters.size)
                        putValueArgument(0, const)

                        if (!isLambda) {
                            val irReceiver = valueParameters.firstOrNull()
                            val receiver = boundCalleeParameters.singleOrNull()
                            //TODO pass proper receiver
                            val receiverValue = receiver?.let {
                                irGet(irReceiver!!.symbol.owner)
                            } ?: irNull()
                            putValueArgument(1, receiverValue)
                        }
                    }

                    // Save all arguments to fields.
                    //TODO don't write receiver again: use it from base class
                    boundCalleeParameters.forEachIndexed { index, it ->
                        +irSetField(
                            irGet(functionReferenceThis.owner),
                            argumentToFieldMap[it]!!,
                            irGet(valueParameters[index])
                        )
                    }
                    +IrInstanceInitializerCallImpl(startOffset, endOffset, functionReferenceClass.symbol, context.irBuiltIns.unitType)
                }
            }

        private fun createInvokeMethod(superFunction: IrSimpleFunction): IrSimpleFunction =
            buildFun {
                setSourceRange(irFunctionReference)
                origin = JvmLoweredDeclarationOrigin.FUNCTION_REFERENCE_IMPL
                name = Name.identifier("invoke")
                visibility = Visibilities.PUBLIC
                returnType = callee.returnType
                isSuspend = superFunction.isSuspend
            }.apply {
                val function = this
                parent = functionReferenceClass
                overriddenSymbols.add(superFunction.symbol)
                dispatchReceiverParameter = functionReferenceClass.thisReceiver?.copyTo(function)

                val unboundArgsSet = unboundCalleeParameters.toSet()
                if (useVararg) {
                    valueParameters.add(superFunction.valueParameters[0].copyTo(function))
                } else {
                    for (param in callee.explicitParameters) {
                        if (param in unboundArgsSet) {
                            val newParam = param.copyTo(
                                function,
                                index = valueParameters.size,
                                type = param.type.substitute(typeArgumentsMap)
                            )
                            valueParameters.add(newParam)
                        }
                    }
                }

                val irBuilder = context.createIrBuilder(function.symbol, startOffset, endOffset)
                body = irBuilder.irBlockBody(startOffset, endOffset) {
                    val arrayGetFun = context.irBuiltIns.arrayClass.owner.functions.find { it.name.asString() == "get" }!!
                    if (useVararg) {
                        val varargParam = valueParameters.single()
                        val arraySizeProperty = context.irBuiltIns.arrayClass.owner.properties.find { it.name.toString() == "size" }!!
                        +irIfThen(
                            irNotEquals(
                                irCall(arraySizeProperty.getter!!).apply {
                                    dispatchReceiver = irGet(varargParam)
                                },
                                irInt(unboundCalleeParameters.size)
                            ),
                            irCall(context.irBuiltIns.illegalArgumentExceptionFun).apply {
                                putValueArgument(0, irString("Expected ${unboundCalleeParameters.size} arguments"))
                            }
                        )
                    }
                    +irReturn(
                        irCall(irFunctionReference.symbol).apply {
                            var unboundIndex = 0

                            calleeParameters.forEach { parameter ->
                                val argument = when {
                                    !unboundArgsSet.contains(parameter) ->
                                        // Bound parameter - read from field.
                                        irGetField(irGet(functionReferenceThis.owner), argumentToFieldMap[parameter]!!)
                                    function.isSuspend && unboundIndex == valueParameters.size ->
                                        // For suspend functions the last argument is continuation and it is implicit.
                                        TODO()
//                                                        irCall(getContinuationSymbol,
//                                                               listOf(ourSymbol.descriptor.returnType!!))
                                    useVararg -> {
                                        val type = parameter.type
                                        val varargParam = valueParameters.single()
                                        irBlock(resultType = type) {
                                            val argValue = irTemporary(
                                                irCall(arrayGetFun).apply {
                                                    dispatchReceiver = irGet(varargParam)
                                                    putValueArgument(0, irInt(unboundIndex++))
                                                }
                                            )
                                            +irIfThen(
                                                irNotIs(irGet(argValue), type),
                                                irCall(context.irBuiltIns.illegalArgumentExceptionFun).apply {
                                                    putValueArgument(0, irString("Wrong type, expected $type"))
                                                }
                                            )
                                            +irGet(argValue)
                                        }
                                    }
                                    else -> {
                                        irGet(valueParameters[unboundIndex++])
                                    }
                                }
                                when (parameter) {
                                    callee.dispatchReceiverParameter -> dispatchReceiver = argument
                                    callee.extensionReceiverParameter -> extensionReceiver = argument
                                    else -> putValueArgument(parameter.index, argument)
                                }
                            }

                            if (!useVararg) assert(unboundIndex == valueParameters.size) { "Not all arguments of <invoke> are used" }
                        }
                    )
                }

            }

        private fun buildField(fieldName: Name, fieldType: IrType): IrField =
            buildField {
                setSourceRange(irFunctionReference)
                origin = JvmLoweredDeclarationOrigin.FUNCTION_REFERENCE_IMPL
                name = fieldName
                type = fieldType
                visibility = JavaVisibilities.PACKAGE_VISIBILITY
                isFinal = true
            }.also {
                functionReferenceClass.declarations.add(it)
            }

        private fun createGetSignatureMethod(superFunction: IrSimpleFunction): IrSimpleFunction =
            buildFun {
                setSourceRange(irFunctionReference)
                origin = JvmLoweredDeclarationOrigin.FUNCTION_REFERENCE_IMPL
                name = Name.identifier("getSignature")
                returnType = superFunction.returnType
                visibility = superFunction.visibility
                modality = superFunction.modality
            }.apply {
                val function = this
                parent = functionReferenceClass
                overriddenSymbols.add(superFunction.symbol)
                dispatchReceiverParameter = functionReferenceClass.thisReceiver!!.copyTo(function)

                val irBuilder = context.createIrBuilder(function.symbol, startOffset, endOffset)
                body = irBuilder.irBlockBody(startOffset, endOffset) {
                    +irReturn(
                        IrConstImpl.string(
                            -1, -1, context.irBuiltIns.stringType,
                            PropertyReferenceCodegen.getSignatureString(
                                irFunctionReference.symbol.descriptor, this@CallableReferenceLowering.context.state
                            )
                        )
                    )
                }
            }

        private fun createGetNameMethod(superNameProperty: IrProperty): IrSimpleFunction {
            val superGetter = superNameProperty.getter!!
            return buildFun {
                setSourceRange(irFunctionReference)
                origin = JvmLoweredDeclarationOrigin.FUNCTION_REFERENCE_IMPL
                name = Name.identifier("getName")
                returnType = superGetter.returnType
                visibility = superGetter.visibility
                modality = superGetter.modality
            }.apply {
                val function = this
                parent = functionReferenceClass
                overriddenSymbols.add(superGetter.symbol)
                dispatchReceiverParameter = functionReferenceClass.thisReceiver?.copyTo(function)

                val irBuilder = context.createIrBuilder(function.symbol, startOffset, endOffset)
                body = irBuilder.irBlockBody(startOffset, endOffset) {
                    +irReturn(
                        IrConstImpl.string(-1, -1, context.irBuiltIns.stringType, callee.name.asString())
                    )
                }
            }
        }

        private fun createGetOwnerMethod(superFunction: IrSimpleFunction): IrSimpleFunction =
            buildFun {
                setSourceRange(functionReferenceClass)
                origin = JvmLoweredDeclarationOrigin.FUNCTION_REFERENCE_IMPL
                name = Name.identifier("getOwner")
                returnType = superFunction.returnType
                visibility = superFunction.visibility
                modality = superFunction.modality
            }.apply {
                val function = this
                parent = functionReferenceClass
                overriddenSymbols.add(superFunction.symbol)
                dispatchReceiverParameter = functionReferenceClass.thisReceiver?.copyTo(function)

                val irBuilder = context.createIrBuilder(function.symbol, startOffset, endOffset)
                body = irBuilder.irBlockBody(startOffset, endOffset) {
                    +irReturn(
                        generateCallableReferenceDeclarationContainer()
                    )
                }
            }

        fun IrBuilderWithScope.generateCallableReferenceDeclarationContainer(): IrExpression {
            val globalContext = this@CallableReferenceLowering.context
            val state = globalContext.state
            val irContainer = callee.parent

            val isContainerPackage =
                ((irContainer as? IrClass)?.origin == IrDeclarationOrigin.FILE_CLASS) || irContainer is IrPackageFragment

            val type = when {
                irContainer is IrClass ->
                    // TODO: getDefaultType() here is wrong and won't work for arrays
                    state.typeMapper.mapType(irContainer.defaultType.toKotlinType())

//                    // TODO: this code is only needed for property references, which are not yet supported.
//                    descriptor is VariableDescriptorWithAccessors -> {
//                        assert(false) { "VariableDescriptorWithAccessors" }
//                        globalContext.state.bindingContext.get(
//                            CodegenBinding.DELEGATED_PROPERTY_METADATA_OWNER, descriptor
//                        )!!
//                    }

                else -> state.typeMapper.mapOwner(callee.descriptor)
            }

            val clazz = globalContext.getIrClass(FqName("java.lang.Class")).owner
            val clazzRef = IrClassReferenceImpl(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                clazz.defaultType,
                clazz.symbol,
                CrIrType(type)
            )

            val reflectionClass = globalContext.getIrClass(FqName("kotlin.jvm.internal.Reflection"))
            return if (isContainerPackage) {
                // Note that this name is not used in reflection. There should be the name of the referenced declaration's module instead,
                // but there's no nice API to obtain that name here yet
                // TODO: write the referenced declaration's module name and use it in reflection
                val module = IrConstImpl.string(
                    -1, -1, globalContext.irBuiltIns.stringType,
                    state.moduleName
                )
                val functionSymbol = reflectionClass.functions.find { it.owner.name.asString() == "getOrCreateKotlinPackage" }!!
                irCall(functionSymbol, functionSymbol.owner.returnType).apply {
                    putValueArgument(0, clazzRef)
                    putValueArgument(1, module)
                }
            } else {
                val functionSymbol = reflectionClass.functions.filter { it.owner.name.asString() == "getOrCreateKotlinClass" }
                    .single { it.owner.valueParameters.size == 1 }
                irCall(functionSymbol, functionSymbol.owner.returnType).apply {
                    putValueArgument(0, clazzRef)
                }
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

    companion object {
        const val MAX_ARGCOUNT_WITHOUT_VARARG = 22
    }
}

// TODO: Move to IrUtils

private fun IrFunction.isInlineFunction(context: JvmBackendContext) =
    (!context.state.isInlineDisabled || typeParameters.any { it.isReified }) &&
            (isInline || isArrayConstructorWithLambda())

private fun IrFunction.isArrayConstructorWithLambda() =
    valueParameters.size == 2 &&
            this is IrConstructor &&
            parentAsClass.let {
                it.getPackageFragment()?.fqName?.asString() == "kotlin" &&
                        it.name.asString().endsWith("Array")
            }

private fun IrValueParameter.isInlineParameter() =
    !isNoinline && !type.isNullable() && type.isFunctionOrKFunction()

private fun IrType.substitute(substitutionMap: Map<IrTypeParameter, IrType>): IrType {
    if (this !is IrSimpleType) return this

    substitutionMap[classifier]?.let { return it }

    val newArguments = arguments.map {
        if (it is IrTypeProjection) {
            makeTypeProjection(it.type.substitute(substitutionMap), it.variance)
        } else {
            it
        }
    }

    val newAnnotations = annotations.map { it.deepCopyWithSymbols() }
    return IrSimpleTypeImpl(
        classifier,
        hasQuestionMark,
        newArguments,
        newAnnotations
    )
}
