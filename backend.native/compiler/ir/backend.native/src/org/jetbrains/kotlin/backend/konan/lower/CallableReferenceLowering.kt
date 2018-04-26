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

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.konan.irasdescriptors.fqNameSafe
import org.jetbrains.kotlin.backend.konan.irasdescriptors.isFunctionOrKFunctionType
import org.jetbrains.kotlin.backend.konan.irasdescriptors.typeWith
import org.jetbrains.kotlin.backend.konan.llvm.functionName
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ClassConstructorDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.LockBasedStorageManager

internal class CallableReferenceLowering(val context: Context): FileLoweringPass {

    private object DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL : IrDeclarationOriginImpl("FUNCTION_REFERENCE_IMPL")

    private var functionReferenceCount = 0

    override fun lower(irFile: IrFile) {
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

            override fun visitDeclaration(declaration: IrDeclaration): IrStatement {
                stack.push(declaration)
                val result = super.visitDeclaration(declaration)
                stack.pop()
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
                    val descriptor = cur.descriptor
                    val argumentDescriptor = descriptor.valueParameters.singleOrNull {
                        cur.getValueArgument(it.index) == argument
                    }
                    if (argumentDescriptor != null && argumentDescriptor.annotations.findAnnotation(FqName("konan.VolatileLambda")) != null) {
                        return expression
                    }
                    break
                }

                if (!expression.type.isFunctionOrKFunctionType) {
                    // Not a subject of this lowering.
                    return expression
                }

                val parent = allScopes.map { it.irElement }.filterIsInstance<IrDeclarationParent>().last()
                val loweredFunctionReference = FunctionReferenceBuilder(
                        currentScope!!.scope.scopeOwner,
                        parent,
                        expression
                ).build()
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
        }, null)
    }

    private class BuiltFunctionReference(val functionReferenceClass: IrClass,
                                         val functionReferenceConstructor: IrConstructor)

    private val COROUTINES_FQ_NAME = FqName.fromSegments(listOf("kotlin", "coroutines", "experimental"))

    private val coroutinesScope    = context.irModule!!.descriptor.getPackage(COROUTINES_FQ_NAME).memberScope
    private val kotlinPackageScope = context.builtIns.builtInsPackageScope

    private val continuationClassDescriptor = coroutinesScope
            .getContributedClassifier(Name.identifier("Continuation"), NoLookupLocation.FROM_BACKEND) as ClassDescriptor

    private val getContinuationSymbol = context.ir.symbols.getContinuation

    private inner class FunctionReferenceBuilder(val containingDeclaration: DeclarationDescriptor,
                                                 val parent: IrDeclarationParent,
                                                 val functionReference: IrFunctionReference) {

        private val functionDescriptor = functionReference.descriptor.original
        private val irFunction = functionReference.symbol.owner
        private val functionParameters = irFunction.explicitParameters
        private val boundFunctionParameters = functionReference.getArgumentsWithIr().map { it.first }
        private val unboundFunctionParameters = functionParameters - boundFunctionParameters

        private lateinit var functionReferenceClassDescriptor: ClassDescriptorImpl
        private lateinit var functionReferenceClass: IrClassImpl
        private lateinit var functionReferenceThis: IrValueParameterSymbol
        private lateinit var argumentToPropertiesMap: Map<ParameterDescriptor, IrField>

        private val kFunctionImplSymbol = context.ir.symbols.kFunctionImpl

        fun build(): BuiltFunctionReference {
            val startOffset = functionReference.startOffset
            val endOffset = functionReference.endOffset

            val returnType = irFunction.returnType
            val superTypes = mutableListOf(kFunctionImplSymbol.typeWith(returnType))

            val numberOfParameters = unboundFunctionParameters.size

            val functionIrClass = context.ir.symbols.kFunctions[numberOfParameters].owner
            val functionParameterTypes = unboundFunctionParameters.map { it.type }
            val functionClassTypeParameters = functionParameterTypes + returnType
            superTypes += functionIrClass.symbol.typeWith(functionClassTypeParameters)

            var suspendFunctionIrClass: IrClass? = null
            var suspendFunctionClassTypeParameters: List<IrType>? = null
            val lastParameterType = unboundFunctionParameters.lastOrNull()?.type
            if (lastParameterType != null && lastParameterType.classifierOrNull?.descriptor == continuationClassDescriptor) {
                lastParameterType as IrSimpleType
                // If the last parameter is Continuation<> inherit from SuspendFunction.
                suspendFunctionIrClass = context.ir.symbols.suspendFunctions[numberOfParameters - 1].owner
                suspendFunctionClassTypeParameters = functionParameterTypes.dropLast(1) +
                        (lastParameterType.arguments.single().typeOrNull ?: context.irBuiltIns.anyNType)
                superTypes += suspendFunctionIrClass.symbol.typeWith(suspendFunctionClassTypeParameters)
            }

            functionReferenceClassDescriptor = object : ClassDescriptorImpl(
                    /* containingDeclaration = */ containingDeclaration,
                    /* name                  = */ "${functionDescriptor.name}\$${functionReferenceCount++}".synthesizedName,
                    /* modality              = */ Modality.FINAL,
                    /* kind                  = */ ClassKind.CLASS,
                    /* superTypes            = */ superTypes.map { it.toKotlinType() },
                    /* source                = */ SourceElement.NO_SOURCE,
                    /* isExternal            = */ false,
                    /* storageManager        = */ LockBasedStorageManager.NO_LOCKS
            ) {
                override fun getVisibility() = Visibilities.PRIVATE
            }
            functionReferenceClass = IrClassImpl(
                    startOffset = startOffset,
                    endOffset   = endOffset,
                    origin      = DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL,
                    descriptor  = functionReferenceClassDescriptor
            )
            functionReferenceClass.parent = this.parent


            val constructorBuilder = createConstructorBuilder()

            val invokeFunctionSymbol =
                    functionIrClass.simpleFunctions().single { it.name.asString() == "invoke" }.symbol
            val invokeMethodBuilder = createInvokeMethodBuilder(invokeFunctionSymbol, functionReferenceClass)

            var suspendInvokeMethodBuilder: SymbolWithIrBuilder<IrSimpleFunctionSymbol, IrSimpleFunction>? = null
            if (suspendFunctionIrClass != null) {
                val suspendInvokeFunctionSymbol =
                        suspendFunctionIrClass.simpleFunctions().single { it.name.asString() == "invoke" }.symbol
                suspendInvokeMethodBuilder = createInvokeMethodBuilder(suspendInvokeFunctionSymbol, functionReferenceClass)
            }

            val memberScope = stub<MemberScope>("callable reference class")
            functionReferenceClassDescriptor.initialize(
                    memberScope, setOf(constructorBuilder.symbol.descriptor), null)

            functionReferenceClass.createParameterDeclarations()

            functionReferenceThis = functionReferenceClass.thisReceiver!!.symbol

            constructorBuilder.initialize()
            functionReferenceClass.addChild(constructorBuilder.ir)

            invokeMethodBuilder.initialize()
            functionReferenceClass.addChild(invokeMethodBuilder.ir)

            suspendInvokeMethodBuilder?.let {
                it.initialize()
                functionReferenceClass.addChild(it.ir)
            }

            functionReferenceClass.setSuperSymbolsAndAddFakeOverrides(superTypes)

            return BuiltFunctionReference(functionReferenceClass, constructorBuilder.ir)
        }

        private fun createConstructorBuilder()
                = object : SymbolWithIrBuilder<IrConstructorSymbol, IrConstructor>() {

            private val kFunctionImplConstructorSymbol = kFunctionImplSymbol.constructors.single()

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
                    parameter.descriptor.copyAsValueParameter(descriptor, index)
                }
                descriptor.initialize(constructorParameters, Visibilities.PUBLIC)
                descriptor.returnType = functionReferenceClassDescriptor.defaultType
            }

            override fun buildIr(): IrConstructor {

                val symbols = this@CallableReferenceLowering.context.ir.symbols
                val irBuiltIns = context.irBuiltIns

                argumentToPropertiesMap = boundFunctionParameters.associate {
                    it.descriptor to buildField(it.name, it.type, false)
                }

                val startOffset = functionReference.startOffset
                val endOffset = functionReference.endOffset
                return IrConstructorImpl(
                        startOffset = startOffset,
                        endOffset   = endOffset,
                        origin      = DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL,
                        symbol      = symbol).apply {

                    returnType = functionReferenceClass.defaultType

                    val irBuilder = context.createIrBuilder(this.symbol, startOffset, endOffset)

                    boundFunctionParameters.mapIndexedTo(this.valueParameters) { index, parameter ->
                        parameter.copy(descriptor.valueParameters[index])
                    }

                    body = irBuilder.irBlockBody {
                        +IrDelegatingConstructorCallImpl(startOffset, endOffset,
                                context.irBuiltIns.unitType,
                                kFunctionImplConstructorSymbol, kFunctionImplConstructorSymbol.descriptor, 0).apply {
                            val stringType = irBuiltIns.stringType
                            val name = IrConstImpl(startOffset, endOffset, stringType,
                                    IrConstKind.String, functionDescriptor.name.asString())
                            putValueArgument(0, name)
                            val fqName = IrConstImpl(startOffset, endOffset, stringType, IrConstKind.String,
                                    (functionReference.symbol.owner).fullName)
                            putValueArgument(1, fqName)
                            val bound = IrConstImpl.boolean(startOffset, endOffset, context.irBuiltIns.booleanType,
                                    boundFunctionParameters.isNotEmpty())
                            putValueArgument(2, bound)
                            val needReceiver = boundFunctionParameters.singleOrNull()?.descriptor is ReceiverParameterDescriptor
                            val receiver = if (needReceiver) irGet(valueParameters.single()) else irNull()
                            putValueArgument(3, receiver)
                        }
                        +IrInstanceInitializerCallImpl(startOffset, endOffset, functionReferenceClass.symbol, irBuiltIns.unitType)
                        // Save all arguments to fields.
                        boundFunctionParameters.forEachIndexed { index, parameter ->
                            +irSetField(irGet(functionReferenceThis.owner), argumentToPropertiesMap[parameter.descriptor]!!, irGet(valueParameters[index]))
                        }
                    }
                }
            }
        }

        private val IrFunction.fullName: String
            get() = parent.fqNameSafe.child(Name.identifier(functionName)).asString()

        private fun createInvokeMethodBuilder(superFunctionSymbol: IrSimpleFunctionSymbol, parent: IrClass)
                = object : SymbolWithIrBuilder<IrSimpleFunctionSymbol, IrSimpleFunction>() {

            val superFunctionDescriptor: FunctionDescriptor = superFunctionSymbol.descriptor

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
                        /* visibility                   = */ Visibilities.PRIVATE).apply {
                    overriddenDescriptors              +=    superFunctionDescriptor
                    isSuspend                           =    superFunctionDescriptor.isSuspend
                }
            }

            override fun buildIr(): IrSimpleFunction {
                val startOffset = functionReference.startOffset
                val endOffset = functionReference.endOffset
                val ourSymbol = symbol
                return IrFunctionImpl(
                        startOffset = startOffset,
                        endOffset   = endOffset,
                        origin      = DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL,
                        symbol      = ourSymbol).apply {

                    returnType = superFunctionSymbol.owner.returnType // FIXME: substitute

                    val function = this
                    val irBuilder = context.createIrBuilder(function.symbol, startOffset, endOffset)

                    this.parent = parent

                    this.createDispatchReceiverParameter()

                    superFunctionSymbol.owner.valueParameters.mapTo(this.valueParameters) {
                        it.copy(descriptor.valueParameters[it.index]) // FIXME: substitute
                    }

                    body = irBuilder.irBlockBody(startOffset, endOffset) {
                        +irReturn(
                                irCall(functionReference.symbol).apply {
                                    var unboundIndex = 0
                                    val unboundArgsSet = unboundFunctionParameters.toSet()
                                    functionParameters.forEach {
                                        val argument =
                                                if (!unboundArgsSet.contains(it))
                                                    // Bound parameter - read from field.
                                                    irGetField(
                                                            irGet(function.dispatchReceiverParameter!!),
                                                            argumentToPropertiesMap[it.descriptor]!!
                                                    )
                                                else {
                                                    if (ourSymbol.descriptor.isSuspend && unboundIndex == valueParameters.size)
                                                        // For suspend functions the last argument is continuation and it is implicit.
                                                        irCall(getContinuationSymbol.owner,
                                                                listOf(returnType))
                                                    else
                                                        irGet(valueParameters[unboundIndex++])
                                                }
                                        when (it) {
                                            irFunction.dispatchReceiverParameter -> dispatchReceiver = argument
                                            irFunction.extensionReceiverParameter -> extensionReceiver = argument
                                            else -> putValueArgument(it.index, argument)
                                        }
                                    }
                                    assert(unboundIndex == valueParameters.size, { "Not all arguments of <invoke> are used" })
                                }
                        )
                    }
                }
            }
        }

        private fun buildField(name: Name, type: IrType, isMutable: Boolean): IrField = createField(
                functionReference.startOffset,
                functionReference.endOffset,
                type,
                name,
                isMutable,
                DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL,
                functionReferenceClassDescriptor
        ).also {
            functionReferenceClass.addChild(it)
        }
    }
}

