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
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.jvm.diagnostics.DelegationToTraitImpl
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes.*
import java.util.*

class InterfaceImplBodyCodegen(
        aClass: KtClassOrObject,
        context: ClassContext,
        v: ClassBuilder,
        state: GenerationState,
        parentCodegen: MemberCodegen<*>?
) : ClassBodyCodegen(aClass, context, InterfaceImplBodyCodegen.InterfaceImplClassBuilder(v), state, parentCodegen) {
    private var isAnythingGenerated: Boolean = false
        get() = (v as InterfaceImplClassBuilder).isAnythingGenerated

    override fun generateDeclaration() {
        v.defineClass(
                myClass, V1_6, ACC_PUBLIC or ACC_FINAL or ACC_SUPER,
                typeMapper.mapDefaultImpls(descriptor).internalName,
                null, "java/lang/Object", ArrayUtil.EMPTY_STRING_ARRAY
        )
        v.visitSource(myClass.containingFile.name, null)
    }

    override fun classForInnerClassRecord(): ClassDescriptor? {
        if (!isAnythingGenerated) return null
        if (DescriptorUtils.isLocal(descriptor)) return null
        val classDescriptorImpl = ClassDescriptorImpl(
                descriptor, Name.identifier(JvmAbi.DEFAULT_IMPLS_CLASS_NAME),
                Modality.FINAL, ClassKind.CLASS, Collections.emptyList(), SourceElement.NO_SOURCE)

        classDescriptorImpl.initialize(MemberScope.Empty, emptySet(), null)
        return classDescriptorImpl
    }

    override fun generateSyntheticParts() {
        for (memberDescriptor in descriptor.defaultType.memberScope.getContributedDescriptors()) {
            if (memberDescriptor !is CallableMemberDescriptor) continue

            if (memberDescriptor.kind.isReal) continue
            if (memberDescriptor.visibility == Visibilities.INVISIBLE_FAKE) continue
            if (memberDescriptor.modality == Modality.ABSTRACT) continue

            val implementation = findImplementationFromInterface(memberDescriptor) ?: continue

            // If implementation is located in a Java interface, it will be inherited via normal Java rules
            if (implementation is JavaMethodDescriptor) continue

            // We create a copy of the function with kind = DECLARATION so that FunctionCodegen will generate its body
            val copy = memberDescriptor.copy(memberDescriptor.containingDeclaration, Modality.OPEN, memberDescriptor.visibility,
                                             CallableMemberDescriptor.Kind.DECLARATION, true)

            if (memberDescriptor is FunctionDescriptor) {
                generateDelegationToSuperTraitImpl(copy as FunctionDescriptor, implementation as FunctionDescriptor)
            }
            else if (memberDescriptor is PropertyDescriptor) {
                implementation as PropertyDescriptor
                val getter = (copy as PropertyDescriptor).getter
                val implGetter = implementation.getter
                if (getter != null && implGetter != null) {
                    generateDelegationToSuperTraitImpl(getter, implGetter)
                }
                val setter = copy.setter
                val implSetter = implementation.setter
                if (setter != null && implSetter != null) {
                    generateDelegationToSuperTraitImpl(setter, implSetter)
                }
            }
        }

        generateSyntheticAccessors()
    }

    private fun generateDelegationToSuperTraitImpl(descriptor: FunctionDescriptor, implementation: FunctionDescriptor) {
        val delegateTo = firstSuperMethodFromKotlin(descriptor, implementation) as FunctionDescriptor? ?: return

        // We can't call super methods from Java 1.8 interfaces because that requires INVOKESPECIAL which is forbidden from TImpl class
        if (delegateTo is JavaMethodDescriptor) return

        functionCodegen.generateMethod(
                DelegationToTraitImpl(DescriptorToSourceUtils.descriptorToDeclaration(descriptor), descriptor),
                descriptor,
                object : FunctionGenerationStrategy.CodegenBased(state) {
                    override fun doGenerateBody(codegen: ExpressionCodegen, signature: JvmMethodSignature) {
                        val iv = codegen.v

                        val method = typeMapper.mapToCallableMethod(delegateTo, true)
                        val myParameters = signature.valueParameters
                        val calleeParameters = method.getValueParameters()

                        if (myParameters.size != calleeParameters.size) {
                            throw AssertionError(
                                    "Method from super interface has a different signature.\n" +
                                    "This method:\n%s\n%s\n%s\nSuper method:\n%s\n%s\n%s".format(
                                            descriptor, signature, myParameters, delegateTo, method, calleeParameters
                                    )
                            )
                        }

                        var k = 0
                        val it = calleeParameters.iterator()
                        for (parameter in myParameters) {
                            val type = parameter.asmType
                            StackValue.local(k, type).put(it.next().asmType, iv)
                            k += type.size
                        }

                        method.genInvokeInstruction(iv)
                        StackValue.coerce(method.returnType, signature.returnType, iv)
                        iv.areturn(signature.returnType)
                    }
                })
    }

    override fun generateKotlinMetadataAnnotation() {
        (v as InterfaceImplClassBuilder).stopCounting()

        writeSyntheticClassMetadata(v)
    }

    override fun done() {
        super.done()
        if (!isAnythingGenerated) {
            state.factory.removeClasses(setOf(typeMapper.mapDefaultImpls(descriptor).internalName))
        }
    }

    private class InterfaceImplClassBuilder(private val v: ClassBuilder) : DelegatingClassBuilder() {
        private var shouldCount: Boolean = true
        var isAnythingGenerated: Boolean = false
            private set

        fun stopCounting() {
            shouldCount = false
        }

        override fun getDelegate() = v

        override fun newMethod(
                origin: JvmDeclarationOrigin,
                access: Int,
                name: String,
                desc: String,
                signature: String?,
                exceptions: Array<out String>?
        ): MethodVisitor {
            if (shouldCount) {
                isAnythingGenerated = true
            }
            return super.newMethod(origin, access, name, desc, signature, exceptions)
        }
    }
}
