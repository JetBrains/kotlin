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
import org.jetbrains.jet.codegen.ClassBuilderFactory
import org.jetbrains.jet.codegen.SignatureCollectingClassBuilderFactory
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.diagnostics.DiagnosticHolder
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.resolve.BindingContextUtils
import org.jetbrains.jet.lang.resolve.java.diagnostics.*
import java.util.*
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor.Kind.DELEGATION
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor.Kind.FAKE_OVERRIDE
import org.jetbrains.jet.utils.addIfNotNull
import org.jetbrains.jet.codegen.ClassBuilderMode
import org.jetbrains.jet.lang.resolve.java.descriptor.SamAdapterDescriptor
import org.jetbrains.jet.lang.resolve.DescriptorToSourceUtils

class BuilderFactoryForDuplicateSignatureDiagnostics(
        builderFactory: ClassBuilderFactory,
        bindingContext: BindingContext,
        private val diagnostics: DiagnosticHolder
) : SignatureCollectingClassBuilderFactory(builderFactory) {

    // Avoid errors when some classes are not loaded for some reason
    private val typeMapper = JetTypeMapper(bindingContext, ClassBuilderMode.LIGHT_CLASSES)

    override fun handleClashingSignatures(data: ConflictingJvmDeclarationsData) {
        val allDelegatedToTraitImpls = data.signatureOrigins.all { it.originKind == JvmDeclarationOriginKind.DELEGATION_TO_TRAIT_IMPL }

        val elements = LinkedHashSet<PsiElement>()
        for (origin in data.signatureOrigins) {
            var element = origin.element
            if (element == null || allDelegatedToTraitImpls) {
                element = data.classOrigin.element
            }
            elements.addIfNotNull(element)
        }

        for (element in elements) {
            diagnostics.report(ErrorsJvm.CONFLICTING_JVM_DECLARATIONS.on(element, data))
        }
    }

    override fun onClassDone(classOrigin: JvmDeclarationOrigin, classInternalName: String?, hasDuplicateSignatures: Boolean) {
        val descriptor = classOrigin.descriptor
        if (descriptor !is ClassDescriptor) return

        val groupedBySignature = groupDescriptorsBySignature(descriptor)

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
                        memberElement = DescriptorToSourceUtils.callableDescriptorToDeclaration(member)
                        if (memberElement == null && member is PropertyAccessorDescriptor) {
                            memberElement = DescriptorToSourceUtils.callableDescriptorToDeclaration(member.getCorrespondingProperty())
                        }
                    }
                }
            }

            val elementToReportOn = memberElement ?: classOrigin.element
            if (elementToReportOn == null) return // TODO: it'd be better to report this error without any element at all

            val data = ConflictingJvmDeclarationsData(classInternalName, classOrigin, rawSignature, members.map { OtherOrigin(it) })
            diagnostics.report(ErrorsJvm.ACCIDENTAL_OVERRIDE.on(elementToReportOn, data))
        }
    }

    private fun groupDescriptorsBySignature(descriptor: ClassDescriptor): MultiMap<RawSignature, CallableMemberDescriptor> {
        val groupedBySignature = MultiMap.create<RawSignature, CallableMemberDescriptor>()

        fun processMember(member: DeclarationDescriptor?) {
            // a member of super is not visible: no override
            if (member is DeclarationDescriptorWithVisibility && member.getVisibility() == Visibilities.INVISIBLE_FAKE) return
            // if a signature clashes with a SAM-adapter or something like that, there's no harm
            if (member is CallableMemberDescriptor && isOrOverridesSamAdapter(member)) return

            if (member is PropertyDescriptor) {
                processMember(member.getGetter())
                processMember(member.getSetter())
            }
            else if (member is FunctionDescriptor) {
                val methodSignature = typeMapper.mapSignature(member)
                val rawSignature = RawSignature(
                        methodSignature.getAsmMethod().getName()!!, methodSignature.getAsmMethod().getDescriptor()!!, MemberKind.METHOD)
                groupedBySignature.putValue(rawSignature, member)
            }
        }

        for (member in descriptor.getDefaultType().getMemberScope().getAllDescriptors()) {
            processMember(member)
        }

        return groupedBySignature
    }

    public fun isOrOverridesSamAdapter(descriptor: CallableMemberDescriptor): Boolean {
        if (descriptor is SamAdapterDescriptor<*>) return true

        return descriptor.getKind() == CallableMemberDescriptor.Kind.FAKE_OVERRIDE
                && descriptor.getOverriddenDescriptors().all { isOrOverridesSamAdapter(it) }
    }
}
