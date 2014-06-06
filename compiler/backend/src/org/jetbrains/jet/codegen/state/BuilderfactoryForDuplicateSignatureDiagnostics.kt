/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.codegen.state

import com.intellij.psi.PsiElement
import com.intellij.util.containers.MultiMap
import kotlin.Function1
import org.jetbrains.annotations.Nullable
import org.jetbrains.jet.codegen.ClassBuilderFactory
import org.jetbrains.jet.codegen.SignatureCollectingClassBuilderFactory
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.diagnostics.DiagnosticHolder
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.resolve.BindingContextUtils
import org.jetbrains.jet.lang.resolve.calls.CallResolverUtil
import org.jetbrains.jet.lang.resolve.java.diagnostics.*
import org.jetbrains.jet.lang.resolve.java.jvmSignature.JvmMethodSignature
import java.util.*
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor.Kind.DELEGATION
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor.Kind.FAKE_OVERRIDE

class BuilderfactoryForDuplicateSignatureDiagnostics(
        builderFactory: ClassBuilderFactory,
        private val typeMapper: JetTypeMapper,
        private val bindingContext: BindingContext,
        private val diagnostics: DiagnosticHolder
) : SignatureCollectingClassBuilderFactory(builderFactory) {

    override fun handleClashingSignatures(data: ConflictingJvmDeclarationsData) {
        val allDelegatedToTraitImpls = data.signatureOrigins.all { it.originKind == JvmDeclarationOriginKind.DELEGATION_TO_TRAIT_IMPL }

        val elements = LinkedHashSet<PsiElement>()
        for (origin in data.signatureOrigins) {
            var element = origin.element
            if (element == null || allDelegatedToTraitImpls) {
                element = data.classOrigin.element
            }
            if (element != null) {
                elements.add(element!!)
            }
        }

        for (element in elements) {
            diagnostics.report(ErrorsJvm.CONFLICTING_JVM_DECLARATIONS.on(element, data))
        }
    }

    override fun onClassDone(classOrigin: JvmDeclarationOrigin, classInternalName: String?, hasDuplicateSignatures: Boolean) {
        val descriptor = classOrigin.descriptor
        if (descriptor !is ClassDescriptor) return

        val groupedBySignature = MultiMap.create<RawSignature, CallableMemberDescriptor>()
        val queue = LinkedList(descriptor.getDefaultType().getMemberScope().getAllDescriptors())
        while (!queue.isEmpty()) {
            val member = queue.poll()
            if (member is DeclarationDescriptorWithVisibility && member.getVisibility() == Visibilities.INVISIBLE_FAKE) {
                // a member of super is not visible: no override
                continue
            }
            if (member is CallableMemberDescriptor && CallResolverUtil.isOrOverridesSynthesized(member)) {
                // if a signature clashes with a SAM-adapter or something like that, there's no harm
                continue
            }
            if (member is PropertyDescriptor) {
                val getter = member.getGetter()
                if (getter != null) {
                    queue.add(getter)
                }
                val setter = member.getSetter()
                if (setter != null) {
                    queue.add(setter)
                }
            }
            else if (member is FunctionDescriptor) {
                val methodSignature = typeMapper.mapSignature(member)
                val rawSignature = RawSignature(methodSignature.getAsmMethod().getName()!!, methodSignature.getAsmMethod().getDescriptor()!!, MemberKind.METHOD)
                groupedBySignature.putValue(rawSignature, member)
            }
        }

        @signatures
        for ((rawSignature, members) in groupedBySignature.entrySet()!!) {
            if (members.size() <= 1) continue

            var memberElement: PsiElement? = null
            var nonFakeCount = 0
            for (member in members) {
                if (member.getKind() != FAKE_OVERRIDE) {
                    nonFakeCount++
                    // If there's more than one real element, the clashing signature is already reported.
                    // Only clashes between fake overrides are interesting here
                    if (nonFakeCount > 1) continue@signatures

                    if (member.getKind() != DELEGATION) {
                        // Delegates don't have declarations in the code
                        memberElement = BindingContextUtils.callableDescriptorToDeclaration(bindingContext, member)
                        if (memberElement == null && member is PropertyAccessorDescriptor) {
                            memberElement = BindingContextUtils.callableDescriptorToDeclaration(bindingContext, member.getCorrespondingProperty())
                        }
                    }
                }
            }

            val elementToReportOn = memberElement ?: classOrigin.element
            if (elementToReportOn == null) return

            val origins = members.map { OtherOrigin(it) }
            val data = ConflictingJvmDeclarationsData(classInternalName, classOrigin, rawSignature, origins)
            diagnostics.report(ErrorsJvm.ACCIDENTAL_OVERRIDE.on(elementToReportOn, data))
        }
    }
}
