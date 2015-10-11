/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen

import com.intellij.util.ArrayUtil
import org.jetbrains.kotlin.backend.common.bridges.findImplementationFromInterface
import org.jetbrains.kotlin.backend.common.bridges.firstSuperMethodFromKotlin
import org.jetbrains.kotlin.codegen.context.ClassContext
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.java.JvmAnnotationNames.KOTLIN_INTERFACE_DEFAULT_IMPLS
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.JetClassOrObject
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.jvm.diagnostics.DelegationToTraitImpl
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.org.objectweb.asm.Opcodes.ACC_FINAL
import org.jetbrains.org.objectweb.asm.Opcodes.ACC_PUBLIC
import org.jetbrains.org.objectweb.asm.Opcodes.ACC_STATIC
import org.jetbrains.org.objectweb.asm.Opcodes.V1_6
import java.util.*

public class InterfaceImplBodyCodegen(
        aClass: JetClassOrObject,
        context: ClassContext,
        v: ClassBuilder,
        state: GenerationState,
        parentCodegen: MemberCodegen<*>?
) : ClassBodyCodegen(aClass, context, v, state, parentCodegen) {

    override fun generateDeclaration() {
        v.defineClass(
                myClass, V1_6, ACC_PUBLIC or ACC_FINAL or ACC_STATIC,
                typeMapper.mapDefaultImpls(descriptor).internalName,
                null, "java/lang/Object", ArrayUtil.EMPTY_STRING_ARRAY
        )
        v.visitSource(myClass.getContainingFile().getName(), null)
    }

    override fun classForInnerClassRecord(): ClassDescriptor? {
        if (DescriptorUtils.isLocal(descriptor)) return null
        val classDescriptorImpl = ClassDescriptorImpl(
                descriptor, Name.identifier(JvmAbi.DEFAULT_IMPLS_CLASS_NAME),
                Modality.FINAL, Collections.emptyList(), SourceElement.NO_SOURCE)

        classDescriptorImpl.initialize(JetScope.Empty, emptySet(), null)
        return classDescriptorImpl
    }

    override fun generateSyntheticParts() {
        for (memberDescriptor in descriptor.getDefaultType().getMemberScope().getAllDescriptors()) {
            if (memberDescriptor !is CallableMemberDescriptor) continue

            if (memberDescriptor.getKind().isReal()) continue
            if (memberDescriptor.getVisibility() == Visibilities.INVISIBLE_FAKE) continue
            if (memberDescriptor.getModality() == Modality.ABSTRACT) continue

            val implementation = findImplementationFromInterface(memberDescriptor) ?: continue

            // If implementation is located in a Java interface, it will be inherited via normal Java rules
            if (implementation is JavaMethodDescriptor) continue

            // We create a copy of the function with kind = DECLARATION so that FunctionCodegen will generate its body
            val copy = memberDescriptor.copy(memberDescriptor.getContainingDeclaration(), Modality.OPEN, memberDescriptor.getVisibility(),
                                             CallableMemberDescriptor.Kind.DECLARATION, true)

            if (memberDescriptor is FunctionDescriptor) {
                generateDelegationToSuperTraitImpl(copy as FunctionDescriptor, implementation as FunctionDescriptor)
            }
            else if (memberDescriptor is PropertyDescriptor) {
                implementation as PropertyDescriptor
                val getter = (copy as PropertyDescriptor).getGetter()
                val implGetter = implementation.getGetter()
                if (getter != null && implGetter != null) {
                    generateDelegationToSuperTraitImpl(getter, implGetter)
                }
                val setter = copy.getSetter()
                val implSetter = implementation.getSetter()
                if (setter != null && implSetter != null) {
                    generateDelegationToSuperTraitImpl(setter, implSetter)
                }
            }
        }
    }

    private fun generateDelegationToSuperTraitImpl(descriptor: FunctionDescriptor, implementation: FunctionDescriptor) {
        val delegateTo = firstSuperMethodFromKotlin(descriptor, implementation) as FunctionDescriptor? ?: return

        // We can't call super methods from Java 1.8 interfaces because that requires INVOKESPECIAL which is forbidden from TImpl class
        if (delegateTo is JavaMethodDescriptor) return

        functionCodegen.generateMethod(
                DelegationToTraitImpl(DescriptorToSourceUtils.descriptorToDeclaration(descriptor), descriptor),
                descriptor,
                object : FunctionGenerationStrategy.CodegenBased<FunctionDescriptor>(state, descriptor) {
                    override fun doGenerateBody(codegen: ExpressionCodegen, signature: JvmMethodSignature) {
                        val iv = codegen.v

                        val method = typeMapper.mapToCallableMethod(delegateTo, true)
                        val myParameters = signature.getValueParameters()
                        val calleeParameters = method.getValueParameters()

                        if (myParameters.size() != calleeParameters.size()) {
                            throw AssertionError(
                                    "Method from super interface has a different signature.\n" +
                                    "This method:\n%s\n%s\n%s\nSuper method:\n%s\n%s\n%s".format(
                                            callableDescriptor, signature, myParameters, delegateTo, method, calleeParameters
                                    )
                            )
                        }

                        var k = 0
                        val it = calleeParameters.iterator()
                        for (parameter in myParameters) {
                            val type = parameter.getAsmType()
                            StackValue.local(k, type).put(it.next().getAsmType(), iv)
                            k += type.getSize()
                        }

                        method.genInvokeInstruction(iv)
                        StackValue.coerce(method.returnType, signature.getReturnType(), iv)
                        iv.areturn(signature.getReturnType())
                    }
                })
    }

    override fun generateKotlinAnnotation() {
        val av = v.newAnnotation(AsmUtil.asmDescByFqNameWithoutInnerClasses(KOTLIN_INTERFACE_DEFAULT_IMPLS), true)
        av.visit(JvmAnnotationNames.VERSION_FIELD_NAME, JvmAbi.VERSION.toArray())
        av.visitEnd()
        AsmUtil.writeKotlinSyntheticClassAnnotation(v, state)
    }
}
