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

import org.jetbrains.kotlin.backend.common.DeclarationContainerLoweringPass
import org.jetbrains.kotlin.backend.common.descriptors.explicitParameters
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.getKonanInternalClass
import org.jetbrains.kotlin.backend.konan.descriptors.isFunctionOrKFunctionType
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.konan.ir.createFakeOverrideDescriptor
import org.jetbrains.kotlin.backend.konan.llvm.functionName
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.util.createParameterDeclarations
import org.jetbrains.kotlin.ir.util.getArguments
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.replace
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

internal class CallableReferenceLowering(val context: Context): DeclarationContainerLoweringPass {

    private var functionReferenceCount = 0

    override fun lower(irDeclarationContainer: IrDeclarationContainer) {
        irDeclarationContainer.declarations.transformFlat { declaration ->
            if (declaration !is IrDeclarationContainer)
                lowerFunctionReferences(irDeclarationContainer, declaration)
            else
                null
        }
    }

    private fun lowerFunctionReferences(irDeclarationContainer: IrDeclarationContainer,
                                        declaration: IrDeclaration): List<IrDeclaration> {
        val containingDeclaration = when (irDeclarationContainer) {
            is IrClass -> irDeclarationContainer.descriptor
            is IrFile -> irDeclarationContainer.packageFragmentDescriptor
            else -> throw AssertionError("Unexpected declaration container: $irDeclarationContainer")
        }
        val createdClasses = mutableListOf<IrDeclaration>()
        declaration.transformChildrenVoid(object: IrElementTransformerVoid() {

            override fun visitClass(declaration: IrClass): IrStatement {
                // Class is a declaration container - it will be visited by the main visitor (CallableReferenceLowering).
                return declaration
            }

            override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
                expression.transformChildrenVoid(this)

                if (!expression.type.isFunctionOrKFunctionType) {
                    // Not a subject of this lowering.
                    return expression
                }

                val loweredFunctionReference = FunctionReferenceBuilder(containingDeclaration, expression).build()
                createdClasses += loweredFunctionReference.functionReferenceClass
                return IrCallImpl(
                        startOffset = expression.startOffset,
                        endOffset   = expression.endOffset,
                        calleeDescriptor  = loweredFunctionReference.functionReferenceConstructorDescriptor).apply {
                    expression.getArguments().forEachIndexed { index, argument ->
                        putValueArgument(index, argument.second)
                    }
                }
            }
        })
        return listOf(declaration) + createdClasses
    }

    private abstract class DescriptorWithIrBuilder<out D : DeclarationDescriptor, out B : IrDeclaration> {

        protected abstract fun buildDescriptor(): D

        protected open fun doInitialize() {}

        protected abstract fun buildIr(): B

        val descriptor by lazy { buildDescriptor() }

        private val builtIr by lazy { buildIr() }
        private var initialized: Boolean = false

        fun initialize() {
            doInitialize()
            initialized = true
        }

        val ir: B
            get() {
                if (!initialized)
                    throw Error("Access to IR before initialization")
                return builtIr
            }
    }

    private class BuiltFunctionReference(val functionReferenceClass: IrClass,
                                         val functionReferenceConstructorDescriptor: ClassConstructorDescriptor)

    private inner class FunctionReferenceBuilder(val containingDeclaration: DeclarationDescriptor,
                                                 val functionReference: IrFunctionReference) {

        private val functionDescriptor = functionReference.descriptor
        private val functionParameters = functionDescriptor.explicitParameters
        private val boundFunctionParameters = functionReference.getArguments().map { it.first }
        private val unboundFunctionParameters = functionParameters - boundFunctionParameters

        private lateinit var functionReferenceClassDescriptor: ClassDescriptorImpl
        private lateinit var argumentToPropertiesMap: Map<ParameterDescriptor, PropertyDescriptor>
        private val functionReferenceMembers = mutableListOf<IrDeclaration>()

        private fun KotlinType.replace(types: List<KotlinType>) = this.replace(types.map(::TypeProjectionImpl))

        private val kFunctionImplClassDescriptor = context.builtIns.getKonanInternalClass("KFunctionImpl")

        fun build(): BuiltFunctionReference {
            val startOffset = functionReference.startOffset
            val endOffset = functionReference.endOffset

            val superTypes = mutableListOf<KotlinType>(
                    kFunctionImplClassDescriptor.defaultType.replace(listOf(functionDescriptor.returnType!!))
            )
            var suspendFunctionClassDescriptor: ClassDescriptor? = null
            val functionClassDescriptor = context.reflectionTypes.getKFunction(unboundFunctionParameters.size)
            val types = unboundFunctionParameters.map { it.type } + functionDescriptor.returnType!!
            superTypes += functionClassDescriptor.defaultType.replace(types)

            functionReferenceClassDescriptor = ClassDescriptorImpl(
                    /* containingDeclaration = */ containingDeclaration,
                    /* name                  = */ "${functionDescriptor.name}\$${functionReferenceCount++}".synthesizedName,
                    /* modality              = */ Modality.FINAL,
                    /* kind                  = */ ClassKind.CLASS,
                    /* superTypes            = */ superTypes,
                    /* source                = */ SourceElement.NO_SOURCE,
                    /* isExternal            = */ false
            )
            val constructorBuilder = createConstructorBuilder()

            val typeSubstitutor = TypeSubstitutor.create(
                    functionClassDescriptor.declaredTypeParameters
                            .withIndex()
                            .associateBy({ it.value.typeConstructor }, { TypeProjectionImpl(types[it.index]) })
            )
            val invokeFunctionDescriptor = functionClassDescriptor.unsubstitutedMemberScope
                    .getContributedFunctions(Name.identifier("invoke"), NoLookupLocation.FROM_BACKEND).single().substitute(typeSubstitutor)!!
            val invokeMethodBuilder = createInvokeMethodBuilder(invokeFunctionDescriptor)

            val inheritedKFunctionImpl = kFunctionImplClassDescriptor.unsubstitutedMemberScope
                    .getContributedDescriptors()
                    .map { it.createFakeOverrideDescriptor(functionReferenceClassDescriptor) }
                    .filterNotNull()
            val contributedDescriptors = (
                    inheritedKFunctionImpl + invokeMethodBuilder.descriptor
                    ).toList()
            functionReferenceClassDescriptor.initialize(SimpleMemberScope(contributedDescriptors), setOf(constructorBuilder.descriptor), null)

            constructorBuilder.initialize()
            functionReferenceMembers.add(constructorBuilder.ir)

            invokeMethodBuilder.initialize()
            functionReferenceMembers.add(invokeMethodBuilder.ir)

            val functionReferenceClass = IrClassImpl(
                    startOffset = startOffset,
                    endOffset   = endOffset,
                    origin      = DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL,
                    descriptor  = functionReferenceClassDescriptor,
                    members     = functionReferenceMembers
            )

            functionReferenceClass.createParameterDeclarations()

            return BuiltFunctionReference(functionReferenceClass, constructorBuilder.descriptor)
        }

        private fun ParameterDescriptor.copyAsValueParameter(newOwner: CallableDescriptor, index: Int)
                = when (this) {
            is ValueParameterDescriptor -> this.copy(newOwner, name, index)
            is ReceiverParameterDescriptor -> ValueParameterDescriptorImpl(
                    containingDeclaration = newOwner,
                    original = null,
                    index = index,
                    annotations = annotations,
                    name = name,
                    outType = type,
                    declaresDefaultValue = false,
                    isCrossinline = false,
                    isNoinline = false,
                    varargElementType = null,
                    source = source
            )
            else -> throw Error("Unexpected parameter descriptor: $this")
        }

        private fun createConstructorBuilder()
                = object : DescriptorWithIrBuilder<ClassConstructorDescriptorImpl, IrConstructor>() {

            private val kFunctionImplConstructorDescriptor = kFunctionImplClassDescriptor.constructors.single()
            private lateinit var constructorParameters: List<ValueParameterDescriptor>

            override fun buildDescriptor(): ClassConstructorDescriptorImpl {
                return ClassConstructorDescriptorImpl.create(
                        /* containingDeclaration = */ functionReferenceClassDescriptor,
                        /* annotations           = */ Annotations.EMPTY,
                        /* isPrimary             = */ false,
                        /* source                = */ SourceElement.NO_SOURCE
                )
            }

            override fun doInitialize() {
                constructorParameters = boundFunctionParameters.mapIndexed { index, parameter ->
                    parameter.copyAsValueParameter(descriptor, index)
                }
                descriptor.initialize(constructorParameters, Visibilities.PUBLIC)
                descriptor.returnType = functionReferenceClassDescriptor.defaultType
            }

            override fun buildIr(): IrConstructor {
                argumentToPropertiesMap = boundFunctionParameters.associate {
                    it to buildPropertyWithBackingField(it.name, it.type, false)
                }

                val startOffset = functionReference.startOffset
                val endOffset = functionReference.endOffset
                val irBuilder = context.createIrBuilder(descriptor, startOffset, endOffset)
                return IrConstructorImpl(
                        startOffset = startOffset,
                        endOffset = endOffset,
                        origin = DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL,
                        descriptor = descriptor).apply {

                    createParameterDeclarations()

                    body = irBuilder.irBlockBody {
                        +IrDelegatingConstructorCallImpl(startOffset, endOffset, kFunctionImplConstructorDescriptor).apply {
                            val name = IrConstImpl<String>(startOffset, endOffset, context.builtIns.stringType,
                                    IrConstKind.String, functionDescriptor.name.asString())
                            putValueArgument(0, name)
                            val fqName = IrConstImpl<String>(startOffset, endOffset, context.builtIns.stringType, IrConstKind.String,
                                    functionDescriptor.fullName)
                            putValueArgument(1, fqName)
                            val bound = IrConstImpl.boolean(startOffset, endOffset, context.builtIns.booleanType,
                                    boundFunctionParameters.isNotEmpty())
                            putValueArgument(2, bound)
                            val needReceiver = boundFunctionParameters.singleOrNull() is ReceiverParameterDescriptor
                            val receiver = if (needReceiver) irGet(constructorParameters.single()) else irNull()
                            putValueArgument(3, receiver)
                        }
                        +IrInstanceInitializerCallImpl(startOffset, endOffset, functionReferenceClassDescriptor)
                        // Save all arguments to fields.
                        boundFunctionParameters.forEachIndexed { index, parameter ->
                            +irSetField(irThis(), argumentToPropertiesMap[parameter]!!, irGet(constructorParameters[index]))
                        }
                    }
                }
            }
        }

        // TODO: what about private package level functions?
        private val DeclarationDescriptor.fullName: String
            get() {
                return when (this) {
                    is PackageFragmentDescriptor -> fqNameSafe.asString()
                    is ClassDescriptor -> containingDeclaration.fullName + "." + fqNameSafe
                    is FunctionDescriptor -> containingDeclaration.fullName + "." + functionName
                    is PropertyDescriptor -> containingDeclaration.fullName + "." + fqNameSafe
                    else -> TODO("$this")
                }
            }

        private fun createInvokeMethodBuilder(functionInvokeFunctionDescriptor: FunctionDescriptor)
                = object : DescriptorWithIrBuilder<SimpleFunctionDescriptorImpl, IrFunction>() {

            override fun buildDescriptor() = SimpleFunctionDescriptorImpl.create(
                    /* containingDeclaration = */ functionReferenceClassDescriptor,
                    /* annotations           = */ Annotations.EMPTY,
                    /* name                  = */ Name.identifier("invoke"),
                    /* kind                  = */ CallableMemberDescriptor.Kind.DECLARATION,
                    /* source                = */ SourceElement.NO_SOURCE)

            override fun doInitialize() {
                val valueParameters = functionInvokeFunctionDescriptor.valueParameters
                        .map { it.copyAsValueParameter(this.descriptor, it.index) }

                this.descriptor.initialize(
                        /* receiverParameterType        = */ null,
                        /* dispatchReceiverParameter    = */ functionReferenceClassDescriptor.thisAsReceiverParameter,
                        /* typeParameters               = */ emptyList(),
                        /* unsubstitutedValueParameters = */ valueParameters,
                        /* unsubstitutedReturnType      = */ functionInvokeFunctionDescriptor.returnType,
                        /* modality                     = */ Modality.FINAL,
                        /* visibility                   = */ Visibilities.PRIVATE).apply {
                    overriddenDescriptors += functionInvokeFunctionDescriptor
                }
            }

            override fun buildIr(): IrFunction {
                val startOffset = functionReference.startOffset
                val endOffset = functionReference.endOffset
                val ourDescriptor = this.descriptor
                val irBuilder = context.createIrBuilder(this.descriptor, startOffset, endOffset)
                return IrFunctionImpl(
                        startOffset = startOffset,
                        endOffset = endOffset,
                        origin = DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL,
                        descriptor = this.descriptor).apply {

                    createParameterDeclarations()

                    body = irBuilder.irBlockBody(startOffset, endOffset) {
                        +irReturn(
                                irCall(functionReference.descriptor).apply {
                                    var unboundIndex = 0
                                    val unboundArgsSet = unboundFunctionParameters.toSet()
                                    functionParameters.forEach {
                                        val argument = if (unboundArgsSet.contains(it))
                                            irGet(ourDescriptor.valueParameters[unboundIndex++])
                                        else
                                            irGet(irThis(), argumentToPropertiesMap[it]!!)
                                        when (it) {
                                            functionDescriptor.dispatchReceiverParameter -> dispatchReceiver = argument
                                            functionDescriptor.extensionReceiverParameter -> extensionReceiver = argument
                                            else -> putValueArgument((it as ValueParameterDescriptor).index, argument)
                                        }
                                    }
                                    assert(unboundIndex == ourDescriptor.valueParameters.size,
                                            { "Not all arguments of <invoke> are used" })
                                }
                        )
                    }
                }
            }
        }

        private fun createPropertyGetterBuilder(propertyDescriptor: PropertyDescriptor, type: KotlinType)
                = object : DescriptorWithIrBuilder<PropertyGetterDescriptorImpl, IrFunction>() {

            override fun buildDescriptor() = PropertyGetterDescriptorImpl(
                    /* correspondingProperty = */ propertyDescriptor,
                    /* annotations           = */ Annotations.EMPTY,
                    /* modality              = */ Modality.FINAL,
                    /* visibility            = */ Visibilities.PRIVATE,
                    /* isDefault             = */ false,
                    /* isExternal            = */ false,
                    /* isInline              = */ false,
                    /* kind                  = */ CallableMemberDescriptor.Kind.DECLARATION,
                    /* original              = */ null,
                    /* source                = */ SourceElement.NO_SOURCE
            )

            override fun doInitialize() {
                descriptor.apply {
                    initialize(type)
                }
            }

            override fun buildIr() = IrFunctionImpl(
                    startOffset = functionReference.startOffset,
                    endOffset = functionReference.endOffset,
                    origin = DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL,
                    descriptor = descriptor).apply {

                createParameterDeclarations()

                body = context.createIrBuilder(descriptor, startOffset, endOffset).irBlockBody {
                    +irReturn(irGetField(irThis(), propertyDescriptor))
                }
            }
        }

        private fun createPropertySetterBuilder(propertyDescriptor: PropertyDescriptor, type: KotlinType)
                = object : DescriptorWithIrBuilder<PropertySetterDescriptorImpl, IrFunction>() {

            override fun buildDescriptor() = PropertySetterDescriptorImpl(
                    /* correspondingProperty = */ propertyDescriptor,
                    /* annotations           = */ Annotations.EMPTY,
                    /* modality              = */ Modality.FINAL,
                    /* visibility            = */ Visibilities.PRIVATE,
                    /* isDefault             = */ false,
                    /* isExternal            = */ false,
                    /* isInline              = */ false,
                    /* kind                  = */ CallableMemberDescriptor.Kind.DECLARATION,
                    /* original              = */ null,
                    /* source                = */ SourceElement.NO_SOURCE
            )

            lateinit var valueParameterDescriptor: ValueParameterDescriptor

            override fun doInitialize() {
                descriptor.apply {
                    valueParameterDescriptor = ValueParameterDescriptorImpl(
                            containingDeclaration = this,
                            original = null,
                            index = 0,
                            annotations = Annotations.EMPTY,
                            name = Name.identifier("value"),
                            outType = type,
                            declaresDefaultValue = false,
                            isCrossinline = false,
                            isNoinline = false,
                            varargElementType = null,
                            source = SourceElement.NO_SOURCE
                    )

                    initialize(valueParameterDescriptor)
                }
            }

            override fun buildIr() = IrFunctionImpl(
                    startOffset = functionReference.startOffset,
                    endOffset = functionReference.endOffset,
                    origin = DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL,
                    descriptor = descriptor).apply {

                createParameterDeclarations()

                body = context.createIrBuilder(descriptor, startOffset, endOffset).irBlockBody {
                    +irSetField(irThis(), propertyDescriptor, irGet(valueParameterDescriptor))
                }
            }
        }

        private fun createPropertyWithBackingFieldBuilder(name: Name, type: KotlinType, isMutable: Boolean)
                = object : DescriptorWithIrBuilder<PropertyDescriptorImpl, IrProperty>() {

            private lateinit var getterBuilder: DescriptorWithIrBuilder<PropertyGetterDescriptorImpl, IrFunction>
            private var setterBuilder: DescriptorWithIrBuilder<PropertySetterDescriptorImpl, IrFunction>? = null

            override fun buildDescriptor() = PropertyDescriptorImpl.create(
                    /* containingDeclaration = */ functionReferenceClassDescriptor,
                    /* annotations           = */ Annotations.EMPTY,
                    /* modality              = */ Modality.FINAL,
                    /* visibility            = */ Visibilities.PRIVATE,
                    /* isVar                 = */ isMutable,
                    /* name                  = */ name,
                    /* kind                  = */ CallableMemberDescriptor.Kind.DECLARATION,
                    /* source                = */ SourceElement.NO_SOURCE,
                    /* lateInit              = */ false,
                    /* isConst               = */ false,
                    /* isHeader              = */ false,
                    /* isImpl                = */ false,
                    /* isExternal            = */ false,
                    /* isDelegated           = */ false)

            override fun doInitialize() {
                getterBuilder = createPropertyGetterBuilder(descriptor, type).apply { initialize() }
                if (isMutable)
                    setterBuilder = createPropertySetterBuilder(descriptor, type).apply { initialize() }
                descriptor.initialize(getterBuilder.descriptor, setterBuilder?.descriptor)
                val receiverType: KotlinType? = null
                descriptor.setType(type, emptyList(), functionReferenceClassDescriptor.thisAsReceiverParameter, receiverType)
            }

            override fun buildIr(): IrProperty {
                val startOffset = functionReference.startOffset
                val endOffset = functionReference.endOffset
                val backingField = IrFieldImpl(
                        startOffset = startOffset,
                        endOffset = endOffset,
                        origin = DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL,
                        descriptor = descriptor)
                return IrPropertyImpl(
                        startOffset = startOffset,
                        endOffset = endOffset,
                        origin = DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL,
                        isDelegated = false,
                        descriptor = descriptor,
                        backingField = backingField,
                        getter = getterBuilder.ir,
                        setter = setterBuilder?.ir)
            }
        }

        private fun buildPropertyWithBackingField(name: Name, type: KotlinType, isMutable: Boolean): PropertyDescriptor {
            val propertyBuilder = createPropertyWithBackingFieldBuilder(name, type, isMutable).apply { initialize() }

            functionReferenceMembers.add(propertyBuilder.ir)
            return propertyBuilder.descriptor
        }

        private object DECLARATION_ORIGIN_FUNCTION_REFERENCE_IMPL :
                IrDeclarationOriginImpl("FUNCTION_REFERENCE_IMPL")
    }
}
