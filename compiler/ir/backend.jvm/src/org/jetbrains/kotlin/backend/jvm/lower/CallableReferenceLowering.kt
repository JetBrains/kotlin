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
import org.jetbrains.kotlin.backend.common.descriptors.*
import org.jetbrains.kotlin.backend.common.ir.createFakeOverrideDescriptor
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.common.makePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.codegen.isInlineCall
import org.jetbrains.kotlin.backend.jvm.codegen.isInlineIrExpression
import org.jetbrains.kotlin.backend.jvm.descriptors.JvmPropertyDescriptorImpl
import org.jetbrains.kotlin.codegen.PropertyReferenceCodegen
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.SourceElement.NO_SOURCE
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JavaVisibilities
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type

//Hack implementation to support CR java types in lower
class CrIrType(val type: Type) : IrType {
    override val annotations = emptyList()
}

//Originally was copied from K/Native
class CallableReferenceLowering(val context: JvmBackendContext) : FileLoweringPass {

    object DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL : IrDeclarationOriginImpl("FUNCTION_REFERENCE_IMPL")

    private var functionReferenceCount = 0

    private val inlineLambdaReferences = mutableSetOf<IrFunctionReference>()

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {

            override fun visitCall(expression: IrCall): IrExpression {
                val descriptor = expression.descriptor
                if (descriptor.isInlineCall(context.state)) {
                    //TODO: more wise filtering
                    descriptor.valueParameters.forEach { valueParameter ->
                        if (InlineUtil.isInlineParameter(valueParameter)) {
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
                    descriptor.containingDeclaration in listOf(
                        context.builtIns.getFunction(argumentsCount),
                        context.reflectionTypes.getKFunction(argumentsCount)
                    )
                ) {
                    val vararg = IrVarargImpl(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        context.ir.symbols.array.typeWith(),
                        context.ir.symbols.any.typeWith(),
                        (0 until argumentsCount).map { i -> expression.getValueArgument(i)!! }
                    )
                    val invokeFunDescriptor = context.getClass(FqName("kotlin.jvm.functions.FunctionN"))
                        .getFunction("invoke", listOf(expression.type.toKotlinType()))
                    val invokeFunSymbol = context.ir.symbols.externalSymbolTable.referenceSimpleFunction(invokeFunDescriptor.original)

                    IrCallImpl(
                        UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                        expression.type,
                        invokeFunSymbol, invokeFunDescriptor,
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

                val loweredFunctionReference = FunctionReferenceBuilder(currentScope!!.scope.scopeOwner, expression).build()
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

    private val kotlinPackageScope = context.builtIns.builtInsPackageScope

    private val continuationClassDescriptor = context.getClass(FqName("kotlin.coroutines.experimental.Continuation"))

    //private val getContinuationSymbol = context.ir.symbols.getContinuation

    private inner class FunctionReferenceBuilder(
        val containingDeclaration: DeclarationDescriptor,
        val irFunctionReference: IrFunctionReference
    ) {

        private val functionDescriptor = irFunctionReference.descriptor
        private val functionParameters = functionDescriptor.explicitParameters
        private val boundFunctionParameters = irFunctionReference.getArguments().map { it.first }
        private val unboundFunctionParameters = functionParameters - boundFunctionParameters

        private lateinit var functionReferenceClassDescriptor: ClassDescriptorImpl
        private lateinit var functionReferenceClass: IrClassImpl
        private lateinit var functionReferenceThis: IrValueParameterSymbol
        private lateinit var argumentToPropertiesMap: Map<ParameterDescriptor, IrFieldSymbol>

        private val isLambda = irFunctionReference.origin == IrStatementOrigin.LAMBDA

        private val functionReferenceOrLambda = if (isLambda) context.ir.symbols.lambdaClass else context.ir.symbols.functionReference

        var useVararg: Boolean = false

        fun build(): BuiltFunctionReference {
            val startOffset = irFunctionReference.startOffset
            val endOffset = irFunctionReference.endOffset

            val returnType = functionDescriptor.returnType!!
            val superTypes: MutableList<KotlinType> = mutableListOf(
                functionReferenceOrLambda.descriptor.defaultType
            )

            val numberOfParameters = unboundFunctionParameters.size
            useVararg = (numberOfParameters > MAX_ARGCOUNT_WITHOUT_VARARG)

            val functionClassDescriptor = if (useVararg)
                context.getClass(FqName("kotlin.jvm.functions.FunctionN"))
            else
                context.getClass(FqName("kotlin.jvm.functions.Function$numberOfParameters"))
            val functionParameterTypes = unboundFunctionParameters.map { it.type }
            val functionClassTypeParameters = if (useVararg)
                listOf(returnType)
            else
                functionParameterTypes + returnType
            superTypes += functionClassDescriptor.defaultType.replace(functionClassTypeParameters)

            var suspendFunctionClassDescriptor: ClassDescriptor? = null
            var suspendFunctionClassTypeParameters: List<KotlinType>? = null
            val lastParameterType = unboundFunctionParameters.lastOrNull()?.type
            if (lastParameterType != null && TypeUtils.getClassDescriptor(lastParameterType) == continuationClassDescriptor) {
                // If the last parameter is Continuation<> inherit from SuspendFunction.
                suspendFunctionClassDescriptor = kotlinPackageScope.getContributedClassifier(
                    Name.identifier("SuspendFunction${numberOfParameters - 1}"), NoLookupLocation.FROM_BACKEND
                ) as ClassDescriptor
                suspendFunctionClassTypeParameters = functionParameterTypes.dropLast(1) + lastParameterType.arguments.single().type
                superTypes += suspendFunctionClassDescriptor.defaultType.replace(suspendFunctionClassTypeParameters)
            }

            functionReferenceClassDescriptor = ClassDescriptorImpl(
                /* containingDeclaration = */ containingDeclaration,
                /* name                  = */ "${functionDescriptor.name}\$${functionReferenceCount++}".synthesizedName,
                /* modality              = */ Modality.FINAL,
                /* kind                  = */ ClassKind.CLASS,
                /* superTypes            = */ superTypes,
                /* source                = */ /*TODO*/ (containingDeclaration as? DeclarationDescriptorWithSource)?.source ?: NO_SOURCE,
                /* isExternal            = */ false,
                                              LockBasedStorageManager.NO_LOCKS
            )
            functionReferenceClass = IrClassImpl(
                startOffset = startOffset,
                endOffset = endOffset,
                origin = DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL,
                descriptor = functionReferenceClassDescriptor
            ).apply {
                createParameterDeclarations()
                val typeTranslator = TypeTranslator(context.ir.symbols.externalSymbolTable, context.state.languageVersionSettings,
                                                    enterTableScope=true)
                val constantValueGenerator = ConstantValueGenerator(context.state.module, context.ir.symbols.externalSymbolTable)
                typeTranslator.constantValueGenerator = constantValueGenerator
                constantValueGenerator.typeTranslator = typeTranslator
                functionReferenceClassDescriptor.typeConstructor.supertypes.mapTo(this.superTypes) {
                    typeTranslator.translateType(it)
                }
            }

            val contributedDescriptors = mutableListOf<DeclarationDescriptor>()
            val constructorBuilder = createConstructorBuilder()
            functionReferenceClassDescriptor.initialize(
                SimpleMemberScope(contributedDescriptors), setOf(constructorBuilder.symbol.descriptor), null
            )

            functionReferenceThis = functionReferenceClass.thisReceiver!!.symbol

            val invokeFunctionDescriptor = functionClassDescriptor.getFunction("invoke", functionClassTypeParameters)

            val invokeMethodBuilder = createInvokeMethodBuilder(invokeFunctionDescriptor)

            constructorBuilder.initialize()
            functionReferenceClass.declarations.add(constructorBuilder.ir)

            invokeMethodBuilder.initialize()
            functionReferenceClass.declarations.add(invokeMethodBuilder.ir)

            if (!isLambda) {
                val getSignatureBuilder =
                    createGetSignatureMethodBuilder(functionReferenceOrLambda.owner.descriptor.getFunction("getSignature", emptyList()))
                val getNameBuilder = createGetNameMethodBuilder(functionReferenceOrLambda.owner.descriptor.getProperty("name", emptyList()))
                val getOwnerBuilder =
                    createGetOwnerMethodBuilder(functionReferenceOrLambda.owner.descriptor.getFunction("getOwner", emptyList()))

                val suspendInvokeMethodBuilder =
                    if (suspendFunctionClassDescriptor != null) {
                        val suspendInvokeFunctionDescriptor =
                            suspendFunctionClassDescriptor.getFunction("invoke", suspendFunctionClassTypeParameters!!)
                        createInvokeMethodBuilder(suspendInvokeFunctionDescriptor)
                    } else null

                val inheritedScope = functionReferenceOrLambda.descriptor.unsubstitutedMemberScope
                    .getContributedDescriptors().mapNotNull { it.createFakeOverrideDescriptor(functionReferenceClassDescriptor) }
                    .filterNot { !isLambda && (it.name.asString() == "getSignature" || it.name.asString() == "name" || it.name.asString() == "getOwner") }

                contributedDescriptors.addAll(
                    (
                            inheritedScope + invokeMethodBuilder.symbol.descriptor +
                                    suspendInvokeMethodBuilder?.symbol?.descriptor + getSignatureBuilder.symbol.descriptor
                            ).filterNotNull()
                )

                getSignatureBuilder.initialize()
                functionReferenceClass.declarations.add(getSignatureBuilder.ir)

                getNameBuilder.initialize()
                functionReferenceClass.declarations.add(getNameBuilder.ir)

                getOwnerBuilder.initialize()
                functionReferenceClass.declarations.add(getOwnerBuilder.ir)

                suspendInvokeMethodBuilder?.let {
                    it.initialize()
                    functionReferenceClass.declarations.add(it.ir)
                }
            }

            return BuiltFunctionReference(functionReferenceClass, constructorBuilder.ir)
        }

        private fun createConstructorBuilder() = object : SymbolWithIrBuilder<IrConstructorSymbol, IrConstructor>() {

            private val kFunctionRefConstructorSymbol =
                functionReferenceOrLambda.constructors.filter { it.descriptor.valueParameters.size == if (isLambda) 1 else 2 }.single()

            override fun buildSymbol() = IrConstructorSymbolImpl(
                ClassConstructorDescriptorImpl.create(
                    /* containingDeclaration = */ functionReferenceClassDescriptor,
                    /* annotations           = */ Annotations.EMPTY,
                    /* isPrimary             = */ false,
                    /* source                = */ SourceElement.NO_SOURCE
                )
            )

            override fun doInitialize() {
                val descriptor = symbol.descriptor as ClassConstructorDescriptorImpl
                val constructorParameters = boundFunctionParameters.mapIndexed { index, parameter ->
                    parameter.copyAsValueParameter(descriptor, index, parameter.name)
                }
                descriptor.initialize(constructorParameters, Visibilities.PUBLIC)
                descriptor.returnType = functionReferenceClassDescriptor.defaultType
            }

            override fun buildIr(): IrConstructor {
                argumentToPropertiesMap = boundFunctionParameters.associate {
                    it to buildPropertyWithBackingField(it.name.safeName(), it.type)
                }

                val startOffset = irFunctionReference.startOffset
                val endOffset = irFunctionReference.endOffset
                return IrConstructorImpl(
                    startOffset = startOffset,
                    endOffset = endOffset,
                    origin = DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL,
                    symbol = symbol,
                    returnType = symbol.descriptor.returnType.toIrType()!!
                ).apply {
                    val irBuilder = context.createIrBuilder(this.symbol, startOffset, endOffset)

                    createParameterDeclarations()

                    body = irBuilder.irBlockBody {
                        +IrDelegatingConstructorCallImpl(
                            startOffset, endOffset, context.irBuiltIns.unitType,
                            kFunctionRefConstructorSymbol, kFunctionRefConstructorSymbol.descriptor
                        ).apply {
                            val const =
                                IrConstImpl.int(startOffset, endOffset, context.irBuiltIns.intType, unboundFunctionParameters.size)
                            putValueArgument(0, const)

                            if (!isLambda) {
                                val irReceiver = valueParameters.firstOrNull()
                                val receiver = boundFunctionParameters.singleOrNull()
                                //TODO pass proper receiver
                                val receiverValue = receiver?.let {
                                    irGet(irReceiver!!.symbol.owner)
                                } ?: irNull()
                                putValueArgument(1, receiverValue)
                            }
                        }

                        //TODO don't write receiver again: use it from base class
                        boundFunctionParameters.forEachIndexed { index, it ->
                            +irSetField(
                                irGet(functionReferenceThis.owner),
                                argumentToPropertiesMap[it]!!.owner,
                                irGet(valueParameters[index])
                            )
                        }
                        +IrInstanceInitializerCallImpl(startOffset, endOffset, functionReferenceClass.symbol, context.irBuiltIns.unitType)
                        // Save all arguments to fields.

                    }
                }
            }
        }

        private fun createInvokeMethodBuilder(superFunctionDescriptor: FunctionDescriptor) =
            object : SymbolWithIrBuilder<IrSimpleFunctionSymbol, IrSimpleFunction>() {

                override fun buildSymbol() = IrSimpleFunctionSymbolImpl(
                    SimpleFunctionDescriptorImpl.create(
                        /* containingDeclaration = */ functionReferenceClassDescriptor,
                        /* annotations           = */ Annotations.EMPTY,
                        /* name                  = */ Name.identifier("invoke"),
                        /* kind                  = */ CallableMemberDescriptor.Kind.DECLARATION,
                        /* source                = */ SourceElement.NO_SOURCE
                    )
                )

                override fun doInitialize() {
                    val descriptor = symbol.descriptor as SimpleFunctionDescriptorImpl
                    val valueParameters = superFunctionDescriptor.valueParameters
                        .map { it.copyAsValueParameter(descriptor, it.index) }

                    descriptor.initialize(
                        /* receiverParameterType        = */ null,
                        /* dispatchReceiverParameter    = */ functionReferenceClassDescriptor.thisAsReceiverParameter,
                        /* typeParameters               = */ emptyList(),
                        /* unsubstitutedValueParameters = */ valueParameters,
                        /* unsubstitutedReturnType      = */ superFunctionDescriptor.returnType,
                        /* modality                     = */ Modality.FINAL,
                        /* visibility                   = */ Visibilities.PUBLIC
                    ).apply {
                        overriddenDescriptors += superFunctionDescriptor
                        isSuspend = superFunctionDescriptor.isSuspend
                    }
                }

                override fun buildIr(): IrSimpleFunction {
                    val startOffset = irFunctionReference.startOffset
                    val endOffset = irFunctionReference.endOffset
                    val ourSymbol = symbol
                    return IrFunctionImpl(
                        startOffset = startOffset,
                        endOffset = endOffset,
                        origin = DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL,
                        symbol = ourSymbol,
                        returnType = ourSymbol.descriptor.returnType!!.toIrType()!!
                    ).apply {

                        val function = this
                        val irBuilder = context.createIrBuilder(function.symbol, startOffset, endOffset)
                        createParameterDeclarations()

                        body = irBuilder.irBlockBody(startOffset, endOffset) {
                            val arrayGetFun = context.irBuiltIns.arrayClass.owner
                                .declarations.find {
                                (it as? IrSimpleFunction)?.name?.toString() == "get"
                            }!! as IrSimpleFunction

                            if (useVararg) {
                                val varargParam = valueParameters.single()
                                val arraySizeProperty = context.irBuiltIns.arrayClass.owner.declarations.find {
                                    (it as? IrProperty)?.name?.toString() == "size"
                                } as IrProperty
                                +irIfThen(
                                    irNotEquals(
                                        irCall(arraySizeProperty.getter!!).apply {
                                            dispatchReceiver = irGet(varargParam)
                                        },
                                        irInt(unboundFunctionParameters.size)
                                    ),
                                    irCall(context.irBuiltIns.illegalArgumentExceptionFun).apply {
                                        putValueArgument(0, irString("Expected ${unboundFunctionParameters.size} arguments"))
                                    }
                                )
                            }
                            +irReturn(
                                irCall(irFunctionReference.symbol).apply {
                                    var unboundIndex = 0
                                    val unboundArgsSet = unboundFunctionParameters.toSet()

                                    functionParameters.forEach { parameter ->
                                        val argument = when {
                                            !unboundArgsSet.contains(parameter) ->
                                                // Bound parameter - read from field.
                                                irGetField(irGet(functionReferenceThis.owner), argumentToPropertiesMap[parameter]!!.owner)
                                            ourSymbol.descriptor.isSuspend && unboundIndex == valueParameters.size ->
                                                // For suspend functions the last argument is continuation and it is implicit.
                                                TODO()
//                                                        irCall(getContinuationSymbol,
//                                                               listOf(ourSymbol.descriptor.returnType!!))
                                            useVararg -> {
                                                val type = parameter.type.toIrType()!!
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
                                            functionDescriptor.dispatchReceiverParameter -> dispatchReceiver = argument
                                            functionDescriptor.extensionReceiverParameter -> extensionReceiver = argument
                                            else -> putValueArgument((parameter as ValueParameterDescriptor).index, argument)
                                        }
                                    }

                                    if (!useVararg) assert(unboundIndex == valueParameters.size) { "Not all arguments of <invoke> are used" }
                                }
                            )
                        }
                    }
                }
            }

        private fun buildPropertyWithBackingField(name: Name, type: KotlinType): IrFieldSymbol {
            val fieldSymbol = IrFieldSymbolImpl(
                JvmPropertyDescriptorImpl.createFinalField(
                    name, type, functionReferenceClassDescriptor,
                    Annotations.EMPTY, JavaVisibilities.PACKAGE_VISIBILITY, Opcodes.ACC_SYNTHETIC, SourceElement.NO_SOURCE
                )
            )
            val field = IrFieldImpl(
                irFunctionReference.startOffset,
                irFunctionReference.endOffset,
                DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL,
                fieldSymbol,
                type.toIrType()!!
            )

            functionReferenceClass.declarations.add(field)
            return fieldSymbol
        }

        private fun createGetNameMethodBuilder(superNameProperty: PropertyDescriptor) =
            object : SymbolWithIrBuilder<IrSimpleFunctionSymbol, IrSimpleFunction>() {

                override fun buildSymbol() = IrSimpleFunctionSymbolImpl(
                    PropertyDescriptorImpl.create(
                        /* containingDeclaration = */ functionReferenceClassDescriptor,
                        /* annotations           = */ Annotations.EMPTY,
                                                      superNameProperty.modality,
                                                      superNameProperty.visibility,
                                                      false,
                        /* name                  = */ superNameProperty.name,
                        /* kind                  = */ CallableMemberDescriptor.Kind.DECLARATION,
                        /* source                = */ SourceElement.NO_SOURCE,
                                                      false, false, false, false, false, false
                    ).let { property ->

                        property.overriddenDescriptors += superNameProperty
                        PropertyGetterDescriptorImpl(
                            property,
                            Annotations.EMPTY,
                            Modality.OPEN,
                            Visibilities.PUBLIC,
                            false, false, false,
                            CallableMemberDescriptor.Kind.DECLARATION,
                            null,
                            SourceElement.NO_SOURCE
                        ).also {
                            property.initialize(it, null)
                            property.setType(
                                /* outType                   = */ superNameProperty.type,
                                /* typeParameters            = */ superNameProperty.typeParameters,
                                /* dispatchReceiverParameter = */ superNameProperty.dispatchReceiverParameter,
                                /* extensionReceiverParameter= */ superNameProperty.extensionReceiverParameter
                            )
                            //overriddenDescriptors += superNameProperty.getter
                        }
                    }
                )

                override fun doInitialize() {
                    val descriptor = symbol.descriptor as PropertyGetterDescriptorImpl
                    descriptor.initialize(superNameProperty.type)
                }

                override fun buildIr(): IrSimpleFunction {
                    val startOffset = irFunctionReference.startOffset
                    val endOffset = irFunctionReference.endOffset
                    val ourSymbol = symbol
                    return IrFunctionImpl(
                        startOffset = startOffset,
                        endOffset = endOffset,
                        origin = DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL,
                        symbol = ourSymbol,
                        returnType = ourSymbol.descriptor.returnType!!.toIrType()!!
                    ).apply {

                        val function = this
                        val irBuilder = context.createIrBuilder(function.symbol, startOffset, endOffset)

                        createParameterDeclarations()

                        body = irBuilder.irBlockBody(startOffset, endOffset) {
                            +irReturn(
                                IrConstImpl.string(-1, -1, context.irBuiltIns.stringType, functionDescriptor.name.asString())
                            )
                        }
                    }
                }
            }


        private fun createGetSignatureMethodBuilder(superFunctionDescriptor: FunctionDescriptor) =
            object : SymbolWithIrBuilder<IrSimpleFunctionSymbol, IrSimpleFunction>() {

                override fun buildSymbol() = IrSimpleFunctionSymbolImpl(
                    SimpleFunctionDescriptorImpl.create(
                        /* containingDeclaration = */ functionReferenceClassDescriptor,
                        /* annotations           = */ Annotations.EMPTY,
                        /* name                  = */ Name.identifier("getSignature"),
                        /* kind                  = */ CallableMemberDescriptor.Kind.DECLARATION,
                        /* source                = */ SourceElement.NO_SOURCE
                    )
                )

                override fun doInitialize() {
                    val descriptor = symbol.descriptor as SimpleFunctionDescriptorImpl
                    val valueParameters = superFunctionDescriptor.valueParameters
                        .map { it.copyAsValueParameter(descriptor, it.index) }

                    descriptor.initialize(
                        /* receiverParameterType        = */ null,
                        /* dispatchReceiverParameter    = */ functionReferenceClassDescriptor.thisAsReceiverParameter,
                        /* typeParameters               = */ emptyList(),
                        /* unsubstitutedValueParameters = */ valueParameters,
                        /* unsubstitutedReturnType      = */ superFunctionDescriptor.returnType,
                        /* modality                     = */ Modality.FINAL,
                        /* visibility                   = */ Visibilities.PUBLIC
                    ).apply {
                        overriddenDescriptors += superFunctionDescriptor
                        isSuspend = superFunctionDescriptor.isSuspend
                    }
                }

                override fun buildIr(): IrSimpleFunction {
                    val startOffset = irFunctionReference.startOffset
                    val endOffset = irFunctionReference.endOffset
                    val ourSymbol = symbol
                    return IrFunctionImpl(
                        startOffset = startOffset,
                        endOffset = endOffset,
                        origin = DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL,
                        symbol = ourSymbol,
                        returnType = ourSymbol.descriptor.returnType!!.toIrType()!!
                    ).apply {

                        val function = this
                        val irBuilder = context.createIrBuilder(function.symbol, startOffset, endOffset)

                        createParameterDeclarations()

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
                }
            }

        private fun createGetOwnerMethodBuilder(superFunctionDescriptor: FunctionDescriptor) =
            object : SymbolWithIrBuilder<IrSimpleFunctionSymbol, IrSimpleFunction>() {

                override fun buildSymbol() = IrSimpleFunctionSymbolImpl(
                    SimpleFunctionDescriptorImpl.create(
                        /* containingDeclaration = */ functionReferenceClassDescriptor,
                        /* annotations           = */ Annotations.EMPTY,
                        /* name                  = */ Name.identifier("getOwner"),
                        /* kind                  = */ CallableMemberDescriptor.Kind.DECLARATION,
                        /* source                = */ SourceElement.NO_SOURCE
                    )
                )

                override fun doInitialize() {
                    val descriptor = symbol.descriptor as SimpleFunctionDescriptorImpl
                    val valueParameters = superFunctionDescriptor.valueParameters
                        .map { it.copyAsValueParameter(descriptor, it.index) }

                    descriptor.initialize(
                        /* receiverParameterType        = */ null,
                        /* dispatchReceiverParameter    = */ functionReferenceClassDescriptor.thisAsReceiverParameter,
                        /* typeParameters               = */ emptyList(),
                        /* unsubstitutedValueParameters = */ valueParameters,
                        /* unsubstitutedReturnType      = */ superFunctionDescriptor.returnType,
                        /* modality                     = */ Modality.FINAL,
                        /* visibility                   = */ Visibilities.PUBLIC
                    ).apply {
                        overriddenDescriptors += superFunctionDescriptor
                        isSuspend = superFunctionDescriptor.isSuspend
                    }
                }

                override fun buildIr(): IrSimpleFunction {
                    val startOffset = irFunctionReference.startOffset
                    val endOffset = irFunctionReference.endOffset
                    val ourSymbol = symbol
                    return IrFunctionImpl(
                        startOffset = startOffset,
                        endOffset = endOffset,
                        origin = DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL,
                        symbol = ourSymbol,
                        returnType = symbol.descriptor.returnType!!.toIrType()!!
                    ).apply {
                        val function = this
                        val irBuilder = context.createIrBuilder(function.symbol, startOffset, endOffset)

                        createParameterDeclarations()

                        body = irBuilder.irBlockBody(startOffset, endOffset) {
                            +irReturn(
                                generateCallableReferenceDeclarationContainer(irFunctionReference)
                            )
                        }
                    }
                }

                fun IrBuilderWithScope.generateCallableReferenceDeclarationContainer(
                    irFunctionReference: IrFunctionReference
                ): IrExpression {
                    val descriptor = irFunctionReference.symbol.descriptor
                    val globalContext = this@CallableReferenceLowering.context
                    val state = globalContext.state
                    val container = descriptor.containingDeclaration

                    val type =
                        when {
                            container is ClassDescriptor ->
                                // TODO: getDefaultType() here is wrong and won't work for arrays
                                state.typeMapper.mapType(container.defaultType)
                            container is PackageFragmentDescriptor ->
                                state.typeMapper.mapOwner(descriptor)
                            descriptor is VariableDescriptorWithAccessors ->
                                globalContext.state.bindingContext.get(
                                    CodegenBinding.DELEGATED_PROPERTY_METADATA_OWNER, descriptor
                                )!!
                            else -> state.typeMapper.mapOwner(descriptor)
                        }

                    val clazzDescriptor = globalContext.getClass(FqName("java.lang.Class"))
                    val clazzSymbol = globalContext.ir.symbols.externalSymbolTable.referenceClass(clazzDescriptor)
                    val clazzRef = IrClassReferenceImpl(
                        UNDEFINED_OFFSET,
                        UNDEFINED_OFFSET,
                        clazzDescriptor.toIrType(),
                        clazzSymbol,
                        CrIrType(type)
                    )

                    val isContainerPackage = if (descriptor is LocalVariableDescriptor)
                        DescriptorUtils.getParentOfType(descriptor, ClassDescriptor::class.java) == null
                    else
                        container is PackageFragmentDescriptor

                    val reflectionClass = globalContext.getClass(FqName("kotlin.jvm.internal.Reflection"))
                    return if (isContainerPackage) {
                        // Note that this name is not used in reflection. There should be the name of the referenced declaration's module instead,
                        // but there's no nice API to obtain that name here yet
                        // TODO: write the referenced declaration's module name and use it in reflection
                        val module = IrConstImpl.string(
                            -1, -1, globalContext.irBuiltIns.stringType,
                            state.moduleName
                        )
                        val functionDescriptor = reflectionClass.getStaticFunction("getOrCreateKotlinPackage", emptyList())
                        val functionSymbol = globalContext.ir.symbols.externalSymbolTable.referenceSimpleFunction(functionDescriptor)
                        irCall(functionSymbol, functionSymbol.owner.returnType).apply {
                            putValueArgument(0, clazzRef)
                            putValueArgument(1, module)
                        }
                    } else {
                        val functionDescriptor = reflectionClass.staticScope
                            .getContributedFunctions(Name.identifier("getOrCreateKotlinClass"), NoLookupLocation.FROM_BACKEND)
                            .single { it.valueParameters.size == 1 }
                        val functionSymbol = globalContext.ir.symbols.externalSymbolTable.referenceSimpleFunction(functionDescriptor)
                        irCall(functionSymbol, functionSymbol.owner.returnType).apply {
                            putValueArgument(0, clazzRef)
                        }
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

// Copied from K/Native IrUtils2.kt
// TODO move IrUtils2.kt to common
private fun IrClass.createParameterDeclarations() {
    thisReceiver = IrValueParameterImpl(
        startOffset, endOffset,
        IrDeclarationOrigin.INSTANCE_RECEIVER,
        descriptor.thisAsReceiverParameter,
        this.symbol.typeWith(this.typeParameters.map { it.defaultType }),
        null
    ).also { valueParameter ->
        valueParameter.parent = this
    }

    assert(typeParameters.isEmpty())
    assert(descriptor.declaredTypeParameters.isEmpty())
}

private val IrTypeParameter.defaultType: IrType get() = this.symbol.defaultType

private val IrTypeParameterSymbol.defaultType: IrType
    get() = IrSimpleTypeImpl(
        this,
        false,
        emptyList(),
        emptyList()
    )


private fun IrClassifierSymbol.typeWith(arguments: List<IrType>): IrSimpleType =
    IrSimpleTypeImpl(
        this,
        false,
        arguments.map { makeTypeProjection(it, Variance.INVARIANT) },
        emptyList()
    )
