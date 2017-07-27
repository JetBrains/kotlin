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

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.bridges.Bridge
import org.jetbrains.kotlin.backend.common.bridges.findInterfaceImplementation
import org.jetbrains.kotlin.backend.common.bridges.generateBridgesForFunctionDescriptor
import org.jetbrains.kotlin.backend.jvm.JvmLoweredStatementOrigin
import org.jetbrains.kotlin.backend.jvm.descriptors.DefaultImplsClassDescriptor
import org.jetbrains.kotlin.backend.jvm.descriptors.JvmFunctionDescriptorImpl
import org.jetbrains.kotlin.codegen.AsmUtil.getVisibilityAccessFlag
import org.jetbrains.kotlin.codegen.AsmUtil.isAbstractMethod
import org.jetbrains.kotlin.codegen.BuiltinSpecialBridgesUtil
import org.jetbrains.kotlin.codegen.FunctionCodegen.isMethodOfAny
import org.jetbrains.kotlin.codegen.FunctionCodegen.isThereOverriddenInKotlinClass
import org.jetbrains.kotlin.codegen.JvmCodegenUtil
import org.jetbrains.kotlin.codegen.JvmCodegenUtil.getDirectMember
import org.jetbrains.kotlin.codegen.OwnerKind
import org.jetbrains.kotlin.codegen.descriptors.FileClassDescriptor
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.DECLARATION
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.load.java.BuiltinMethodsWithSpecialGenericSignature
import org.jetbrains.kotlin.load.java.getOverriddenBuiltinReflectingJvmDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils.getSourceFromDescriptor
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Opcodes.*
import org.jetbrains.org.objectweb.asm.commons.Method

class BridgeLowering(val state: GenerationState) : ClassLoweringPass {

    val typeMapper = state.typeMapper

    private val IS_PURE_INTERFACE_CHECKER = fun(descriptor: DeclarationDescriptor): Boolean {
        return JvmCodegenUtil.isAnnotationOrJvmInterfaceWithoutDefaults(descriptor, state)
    }

    override fun lower(irClass: IrClass) {
        val classDescriptor = irClass.descriptor
        if (JvmCodegenUtil.isAnnotationOrJvmInterfaceWithoutDefaults(classDescriptor, state) || classDescriptor is FileClassDescriptor) return

        if (classDescriptor is DefaultImplsClassDescriptor) {
            return /*TODO?*/
        }

        val functions = irClass.declarations.filterIsInstance<IrFunction>().filterNot {
            val descriptor = it.descriptor
            descriptor is ConstructorDescriptor ||
            DescriptorUtils.isStaticDeclaration(descriptor)
        }

        functions.forEach {
            generateBridges(it.descriptor, irClass)
        }


        //additional bridges for interface methods
        if (!DescriptorUtils.isInterface(classDescriptor) && classDescriptor !is DefaultImplsClassDescriptor) {
            for (memberDescriptor in DescriptorUtils.getAllDescriptors(classDescriptor.defaultType.memberScope)) {
                if (memberDescriptor is CallableMemberDescriptor) {
                    if (!memberDescriptor.kind.isReal && findInterfaceImplementation(memberDescriptor) == null) {
                        if (memberDescriptor is FunctionDescriptor) {
                            generateBridges(memberDescriptor, irClass)
                        }
                        else if (memberDescriptor is PropertyDescriptor) {
                            val getter = memberDescriptor.getter
                            if (getter != null) {
                                generateBridges(getter, irClass)
                            }
                            val setter = memberDescriptor.setter
                            if (setter != null) {
                                generateBridges(setter, irClass)
                            }
                        }
                    }
                }
            }
        }
    }


    private fun generateBridges(descriptor: FunctionDescriptor, irClass: IrClass) {
        // equals(Any?), hashCode(), toString() never need bridges
        if (isMethodOfAny(descriptor)) {
            return
        }
        val isSpecial = descriptor.getOverriddenBuiltinReflectingJvmDescriptor<CallableMemberDescriptor>() != null

        val bridgesToGenerate: Set<Bridge<SignatureAndDescriptor>>
        if (!isSpecial) {
            bridgesToGenerate = generateBridgesForFunctionDescriptor(
                    descriptor,
                    getSignatureMapper(typeMapper),
                    IS_PURE_INTERFACE_CHECKER
            )
            if (!bridgesToGenerate.isEmpty()) {
                val origin = if (descriptor.kind == DECLARATION) getSourceFromDescriptor(descriptor) else null
                val isSpecialBridge = BuiltinMethodsWithSpecialGenericSignature.getOverriddenBuiltinFunctionWithErasedValueParametersInJava(descriptor) != null

                for (bridge in bridgesToGenerate) {
                    irClass.declarations.add(createBridge(origin, descriptor, bridge.from, bridge.to, isSpecialBridge, false))
                }
            }
        }
        else {
            val specials = BuiltinSpecialBridgesUtil.generateBridgesForBuiltinSpecial(
                    descriptor,
                    getSignatureMapper(typeMapper),
                    IS_PURE_INTERFACE_CHECKER
            )

            if (!specials.isEmpty()) {
                val origin = if (descriptor.kind == DECLARATION) getSourceFromDescriptor(descriptor) else null
                for (bridge in specials) {
                    irClass.declarations.add(createBridge(
                            origin, descriptor, bridge.from, bridge.to,
                            bridge.isSpecial, bridge.isDelegateToSuper))
                }
            }

            if (!descriptor.kind.isReal && isAbstractMethod(descriptor, OwnerKind.IMPLEMENTATION, state)) {
                val overridden = descriptor.getOverriddenBuiltinReflectingJvmDescriptor<CallableMemberDescriptor>()!!

                if (!isThereOverriddenInKotlinClass(descriptor)) {
                    val method = typeMapper.mapAsmMethod(descriptor)
                    val flags = Opcodes.ACC_ABSTRACT or getVisibilityAccessFlag(descriptor)
                    //TODO
                    //v.newMethod(OtherOrigin(overridden!!), flags, method.name, method.descriptor, null, null)
                }
            }
        }
    }

    private fun createBridge(
            origin: PsiElement?,
            descriptor: FunctionDescriptor,
            bridge: SignatureAndDescriptor,
            delegateTo: SignatureAndDescriptor,
            isSpecialBridge: Boolean,
            isStubDeclarationWithDelegationToSuper: Boolean
    ): IrFunction {
        val isSpecialOrDelegationToSuper = isSpecialBridge || isStubDeclarationWithDelegationToSuper
        val flags = ACC_PUBLIC or ACC_BRIDGE or (if (!isSpecialOrDelegationToSuper) ACC_SYNTHETIC else 0) or if (isSpecialBridge) ACC_FINAL else 0 // TODO.
        val containingClass = descriptor.containingDeclaration as ClassDescriptor
        //here some 'isSpecialBridge' magic
        val newName = Name.identifier(bridge.method.name)
        val newDescriptor = JvmFunctionDescriptorImpl(containingClass, null, Annotations.EMPTY, newName, CallableMemberDescriptor.Kind.SYNTHESIZED, descriptor.source, flags)

        val descriptorForBridge = bridge.descriptor
        newDescriptor.initialize(
                null, containingClass.thisAsReceiverParameter, emptyList(),
                descriptorForBridge.valueParameters.map { it.copy(newDescriptor, it.name, it.index) },
                descriptorForBridge.returnType, Modality.OPEN, descriptor.visibility
        )
        val irBody = IrBlockBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET)
        val irFunction = IrFunctionImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, IrDeclarationOrigin.DEFINED, newDescriptor, irBody)

        //TODO
        //MemberCodegen.markLineNumberForDescriptor(owner.getThisDescriptor(), iv)
//        if (delegateTo.method.argumentTypes.isNotEmpty() && isSpecialBridge) {
//            generateTypeCheckBarrierIfNeeded(iv, descriptor, bridge.getReturnType(), delegateTo.getArgumentTypes())
//        }

        //TODO: rewrite
        //here some 'isSpecialBridge' magic (see type special descriptor processing in KotlinTypeMapper)
        val implementation = if (isSpecialBridge) delegateTo.descriptor.copyAsDeclaration() else delegateTo.descriptor
        val call = IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                              implementation,
                              null, JvmLoweredStatementOrigin.BRIDGE_DELEGATION, null)
        call.dispatchReceiver = IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, containingClass.thisAsReceiverParameter, JvmLoweredStatementOrigin.BRIDGE_DELEGATION)
        newDescriptor.valueParameters.mapIndexed { i, valueParameterDescriptor ->
            call.putValueArgument(i, IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, valueParameterDescriptor, JvmLoweredStatementOrigin.BRIDGE_DELEGATION))
        }
        irBody.statements.add(IrReturnImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, newDescriptor, call))

        return irFunction
    }

    companion object {
        fun getSignatureMapper(typeMapper: KotlinTypeMapper): Function1<FunctionDescriptor, SignatureAndDescriptor> {
            return fun(descriptor: FunctionDescriptor): SignatureAndDescriptor {
                return SignatureAndDescriptor(typeMapper.mapAsmMethod(descriptor), descriptor)
            }
        }

        fun FunctionDescriptor.copyAsDeclaration(): FunctionDescriptor {
            val isGetter = this is PropertyGetterDescriptor
            val isAccessor = this is PropertyAccessorDescriptor
            val directMember = getDirectMember(this)
            val copy = directMember.copy(directMember.containingDeclaration, directMember.modality, directMember.visibility, DECLARATION, false)
            if (isAccessor) {
                val property = copy as PropertyDescriptor
                return if (isGetter) property.getter!! else property.setter!!
            }
            return copy as FunctionDescriptor
        }
    }


    class SignatureAndDescriptor(val method: Method, val descriptor: FunctionDescriptor) {
        override fun equals(other: Any?): Boolean {
            val sig2 = other as SignatureAndDescriptor
            return method == sig2.method
        }

        override fun hashCode(): Int {
            return method.hashCode()
        }
    }

}

