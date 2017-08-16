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
import org.jetbrains.kotlin.load.java.descriptors.JavaMethodDescriptor
import org.jetbrains.kotlin.psi.KtPureClassOrObject
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes.*

class InterfaceImplBodyCodegen(
        aClass: KtPureClassOrObject,
        context: ClassContext,
        v: ClassBuilder,
        state: GenerationState,
        parentCodegen: MemberCodegen<*>?
) : ClassBodyCodegen(aClass, context, InterfaceImplBodyCodegen.InterfaceImplClassBuilder(v), state, parentCodegen) {
    private var isAnythingGenerated: Boolean = false
        get() = (v as InterfaceImplClassBuilder).isAnythingGenerated

    private val defaultImplType = typeMapper.mapDefaultImpls(descriptor)

    override fun generateDeclaration() {
        val codegenFlags = ACC_PUBLIC or ACC_FINAL or ACC_SUPER
        val flags = if (state.classBuilderMode == ClassBuilderMode.LIGHT_CLASSES) codegenFlags or ACC_STATIC else codegenFlags
        v.defineClass(
                myClass.psiOrParent, state.classFileVersion, flags,
                defaultImplType.internalName,
                null, "java/lang/Object", ArrayUtil.EMPTY_STRING_ARRAY
        )
        v.visitSource(myClass.containingKtFile.name, null)
    }

    override fun classForInnerClassRecord(): ClassDescriptor? {
        if (!isAnythingGenerated) return null
        return InnerClassConsumer.classForInnerClassRecord(descriptor, true)
    }

    override fun generateSyntheticPartsAfterBody() {
        for (memberDescriptor in descriptor.defaultType.memberScope.getContributedDescriptors()) {
            if (memberDescriptor !is CallableMemberDescriptor) continue

            if (memberDescriptor.kind.isReal) continue
            if (memberDescriptor.visibility == Visibilities.INVISIBLE_FAKE) continue
            if (memberDescriptor.modality == Modality.ABSTRACT) continue

            val implementation = findImplementationFromInterface(memberDescriptor) ?: continue

            // If implementation is a default interface method (JVM 8 only)
            if (implementation.isDefinitelyNotDefaultImplsMethod()) continue

            // We create a copy of the function with kind = DECLARATION so that FunctionCodegen will generate its body
            val copy = memberDescriptor.copy(memberDescriptor.containingDeclaration, Modality.OPEN, memberDescriptor.visibility,
                                             CallableMemberDescriptor.Kind.DECLARATION, true)

            if (memberDescriptor is FunctionDescriptor) {
                generateDelegationToSuperDefaultImpls(copy as FunctionDescriptor, implementation as FunctionDescriptor)
            }
            else if (memberDescriptor is PropertyDescriptor) {
                implementation as PropertyDescriptor
                val getter = (copy as PropertyDescriptor).getter
                val implGetter = implementation.getter
                if (getter != null && implGetter != null) {
                    generateDelegationToSuperDefaultImpls(getter, implGetter)
                }
                val setter = copy.setter
                val implSetter = implementation.setter
                if (setter != null && implSetter != null) {
                    generateDelegationToSuperDefaultImpls(setter, implSetter)
                }
            }
        }

        generateSyntheticAccessors()
    }

    private fun generateDelegationToSuperDefaultImpls(descriptor: FunctionDescriptor, implementation: FunctionDescriptor) {
        val delegateTo = firstSuperMethodFromKotlin(descriptor, implementation) as FunctionDescriptor? ?: return

        // We can't call super methods from Java 1.8 interfaces because that requires INVOKESPECIAL which is forbidden from TImpl class
        if (delegateTo is JavaMethodDescriptor) return

        functionCodegen.generateMethod(
                JvmDeclarationOrigin(
                        JvmDeclarationOriginKind.DEFAULT_IMPL_DELEGATION_TO_SUPERINTERFACE_DEFAULT_IMPL,
                        DescriptorToSourceUtils.descriptorToDeclaration(descriptor), descriptor
                ),
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

        writeSyntheticClassMetadata(v, state)
    }

    override fun done() {
        super.done()
        if (!isAnythingGenerated) {
            state.factory.removeClasses(setOf(defaultImplType.internalName))
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

    override fun generateSyntheticPartsBeforeBody() {
        generatePropertyMetadataArrayFieldIfNeeded(defaultImplType)
    }
}
