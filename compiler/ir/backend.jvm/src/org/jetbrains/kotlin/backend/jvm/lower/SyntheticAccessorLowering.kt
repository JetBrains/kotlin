/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.intrinsics.receiverAndArgs
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.context.ClassContext
import org.jetbrains.kotlin.codegen.context.CodegenContext
import org.jetbrains.kotlin.codegen.descriptors.FileClassDescriptor
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ClassConstructorDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.impl.createFunctionSymbol
import org.jetbrains.kotlin.ir.util.createParameterDeclarations
import org.jetbrains.kotlin.ir.util.usesDefaultArguments
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.KotlinType

interface StubContext {
    val irClassContext: IrClassContext
}

class StubCodegenContext(
    contextDescriptor: ClassDescriptor,
    parentContext: CodegenContext<*>?,
    override val irClassContext: IrClassContext
) : StubContext, CodegenContext<DeclarationDescriptor>(
    if (contextDescriptor is FileClassDescriptor) contextDescriptor.containingDeclaration else contextDescriptor,
    OwnerKind.IMPLEMENTATION, parentContext, null,
    if (contextDescriptor is FileClassDescriptor) null else contextDescriptor,
    null
)

class ClassStubContext(
    contextDescriptor: ClassDescriptor,
    parentContext: CodegenContext<*>?,
    override val irClassContext: IrClassContext,
    typeMapper: KotlinTypeMapper
) : StubContext, ClassContext(typeMapper, contextDescriptor, OwnerKind.IMPLEMENTATION, parentContext, null)

class ContextAnnotator(val state: GenerationState) : ClassLowerWithContext() {

    val context2Codegen = hashMapOf<IrClassContext, CodegenContext<*>>()
    val class2Codegen = hashMapOf<ClassDescriptor, CodegenContext<*>>()

    private val IrClassContext.codegenContext: CodegenContext<*>
        get() = context2Codegen[this]!!


    override fun lowerBefore(irClass: IrClass, data: IrClassContext) {
        val descriptor = irClass.descriptor
        val newContext: CodegenContext<*> = if (descriptor is FileClassDescriptor) {
            StubCodegenContext(descriptor, data.parent?.codegenContext, data)
        } else {
            ClassStubContext(descriptor, data.parent?.codegenContext, data, state.typeMapper)
        }
        newContext.apply {
            context2Codegen.put(data, this)
            class2Codegen.put(descriptor, this)
        }
    }

    override fun lower(irCLass: IrClass, data: IrClassContext) {

    }
}

class SyntheticAccessorLowering(val context: JvmBackendContext) : FileLoweringPass, IrElementTransformer<IrClassContext?> {

    private val state = context.state

    var pendingTransformations = mutableListOf<Function0<Unit>>()

    private val IrClassContext.codegenContext: CodegenContext<*>
        get() = contextAnnotator.context2Codegen[this]!!

    private lateinit var contextAnnotator: ContextAnnotator

    private val ClassDescriptor.codegenContext: CodegenContext<*>
        get() = contextAnnotator.class2Codegen[this]!!

    override fun lower(irFile: IrFile) {
        contextAnnotator = ContextAnnotator(state)
        contextAnnotator.lower(irFile)
        irFile.transform(this, null)

        pendingTransformations.forEach { it() }
    }

    override fun visitClass(declaration: IrClass, data: IrClassContext?): IrStatement {
        val classContext = (declaration.descriptor.codegenContext as StubContext).irClassContext
        return super.visitClass(declaration, classContext).apply {
            pendingTransformations.add { lower(classContext) }
        }
    }

    fun lower(data: IrClassContext) {
        val codegenContext = data.codegenContext
        val accessors = codegenContext.accessors
        val allAccessors =
            (
                    accessors.filterIsInstance<FunctionDescriptor>() +
                            accessors.filterIsInstance<AccessorForPropertyDescriptor>().flatMap {
                                listOfNotNull(
                                    if (it.isWithSyntheticGetterAccessor) it.getter else null,
                                    if (it.isWithSyntheticSetterAccessor) it.setter else null
                                )
                            }
                    ).filterIsInstance<AccessorForCallableDescriptor<*>>()

        val irClassToAddAccessor = data.irClass
        allAccessors.forEach { accessor ->
            addAccessorToClass(accessor, irClassToAddAccessor, context)
        }
    }


    override fun visitMemberAccess(expression: IrMemberAccessExpression, data: IrClassContext?): IrElement {
        val superResult = super.visitMemberAccess(expression, data)
        return createSyntheticAccessorCallForFunction(superResult, expression, data?.codegenContext, context)
    }

    companion object {
        fun createSyntheticAccessorCallForFunction(
            superResult: IrElement,
            expression: IrMemberAccessExpression,
            codegenContext: CodegenContext<*>?,
            context: JvmBackendContext
        ): IrElement {

            val descriptor = expression.descriptor
            if (descriptor is FunctionDescriptor && !expression.usesDefaultArguments()) {
                val directAccessor = codegenContext!!.accessibleDescriptor(
                    JvmCodegenUtil.getDirectMember(descriptor),
                    (expression as? IrCall)?.superQualifier
                )
                val accessor = actualAccessor(descriptor, directAccessor)

                if (accessor is AccessorForCallableDescriptor<*> && descriptor !is AccessorForCallableDescriptor<*>) {
                    val isConstructor = descriptor is ConstructorDescriptor
                    val accessorOwner = accessor.containingDeclaration as ClassOrPackageFragmentDescriptor
                    val accessorForIr =
                        accessorToIrAccessorDescriptor(isConstructor, accessor, context, descriptor, accessorOwner) //TODO change call

                    val call =
                        if (isConstructor && expression is IrDelegatingConstructorCall)
                            IrDelegatingConstructorCallImpl(
                                expression.startOffset,
                                expression.endOffset,
                                accessorForIr as ClassConstructorDescriptor
                            )
                        else IrCallImpl(
                            expression.startOffset,
                            expression.endOffset,
                            accessorForIr,
                            emptyMap(),
                            expression.origin/*TODO super*/
                        )
                    //copyAllArgsToValueParams(call, expression)
                    val receiverAndArgs = expression.receiverAndArgs()
                    receiverAndArgs.forEachIndexed { i, irExpression ->
                        call.putValueArgument(i, irExpression)
                    }
                    if (isConstructor) {
                        call.putValueArgument(
                            receiverAndArgs.size,
                            IrConstImpl.constNull(
                                UNDEFINED_OFFSET,
                                UNDEFINED_OFFSET,
                                context.ir.symbols.defaultConstructorMarker.descriptor.defaultType
                            )
                        )
                    }
                    return call
                }
            }
            return superResult
        }

        private fun accessorToIrAccessorDescriptor(
            isConstructor: Boolean,
            accessor: CallableMemberDescriptor,
            context: JvmBackendContext,
            descriptor: FunctionDescriptor,
            accessorOwner: ClassOrPackageFragmentDescriptor
        ): FunctionDescriptor {
            return if (isConstructor)
                (accessor as AccessorForConstructorDescriptor).constructorDescriptorWithMarker(
                    context.ir.symbols.defaultConstructorMarker.descriptor.defaultType
                )
            else descriptor.toStatic(
                accessorOwner,
                Name.identifier(context.state.typeMapper.mapAsmMethod(accessor as FunctionDescriptor).name)
            )
        }

        fun addAccessorToClass(accessor: AccessorForCallableDescriptor<*>, irClassToAddAccessor: IrClass, context: JvmBackendContext) {
            if (accessor is PropertySetterDescriptor && !accessor.correspondingProperty.isVar) return
            val accessorOwner = (accessor as FunctionDescriptor).containingDeclaration as ClassOrPackageFragmentDescriptor
            val body = IrBlockBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET)
            val isConstructor = accessor.calleeDescriptor is ConstructorDescriptor
            val accessorForIr = accessorToIrAccessorDescriptor(
                isConstructor, accessor, context,
                accessor.calleeDescriptor as? FunctionDescriptor ?: return,
                accessorOwner
            )
            val syntheticFunction = if (isConstructor) IrConstructorImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR,
                accessorForIr as ClassConstructorDescriptor, body
            ) else IrFunctionImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, JvmLoweredDeclarationOrigin.SYNTHETIC_ACCESSOR,
                accessorForIr, body
            )
            syntheticFunction.createParameterDeclarations()

            val calleeDescriptor = accessor.calleeDescriptor as FunctionDescriptor
            val delegationCall =
                if (!isConstructor)
                    IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, calleeDescriptor)
                else IrDelegatingConstructorCallImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    createFunctionSymbol(accessor.calleeDescriptor) as IrConstructorSymbol,
                    accessor.calleeDescriptor as ClassConstructorDescriptor
                )
            copyAllArgsToValueParams(delegationCall, syntheticFunction)

            body.statements.add(
                if (isConstructor) delegationCall else IrReturnImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    syntheticFunction.symbol,
                    delegationCall
                )
            )
            irClassToAddAccessor.declarations.add(syntheticFunction)
        }

        private fun actualAccessor(descriptor: FunctionDescriptor, calculatedAccessor: CallableMemberDescriptor): CallableMemberDescriptor {
            if (calculatedAccessor is AccessorForPropertyDescriptor) {
                val isGetter = descriptor is PropertyGetterDescriptor
                val propertyAccessor = if (isGetter) calculatedAccessor.getter!! else calculatedAccessor.setter!!
                if (isGetter && calculatedAccessor.isWithSyntheticGetterAccessor || !isGetter && calculatedAccessor.isWithSyntheticSetterAccessor) {
                    return propertyAccessor
                }
                return descriptor

            }
            return calculatedAccessor
        }

        private fun copyAllArgsToValueParams(
            call: IrMemberAccessExpression,
            syntheticFunction: IrFunction
        ) {
            var offset = 0
            val delegateTo = call.descriptor
            delegateTo.dispatchReceiverParameter?.let {
                call.dispatchReceiver =
                        IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, syntheticFunction.valueParameters[offset++].symbol)
            }

            delegateTo.extensionReceiverParameter?.let {
                call.extensionReceiver =
                        IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, syntheticFunction.valueParameters[offset++].symbol)
            }

            call.descriptor.valueParameters.forEachIndexed { i, _ ->
                call.putValueArgument(
                    i,
                    IrGetValueImpl(
                        UNDEFINED_OFFSET,
                        UNDEFINED_OFFSET,
                        syntheticFunction.valueParameters[i + offset].symbol
                    )
                )
            }
        }

        private fun AccessorForConstructorDescriptor.constructorDescriptorWithMarker(marker: KotlinType) =
            ClassConstructorDescriptorImpl.createSynthesized(containingDeclaration, annotations, false, source).also {
                it.initialize(
                    DescriptorUtils.getReceiverParameterType(extensionReceiverParameter),
                    dispatchReceiverParameter,
                    emptyList()/*TODO*/,
                    calleeDescriptor.valueParameters.map {
                        it.copy(
                            this,
                            it.name,
                            it.index
                        )
                    } + ValueParameterDescriptorImpl.createWithDestructuringDeclarations(
                        it,
                        null,
                        calleeDescriptor.valueParameters.size,
                        Annotations.EMPTY,
                        Name.identifier("marker"),
                        marker,
                        false,
                        false,
                        false,
                        null,
                        SourceElement.NO_SOURCE,
                        null
                    ),
                    calleeDescriptor.returnType,
                    Modality.FINAL,
                    Visibilities.LOCAL
                )
            }
    }
}