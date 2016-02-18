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

package org.jetbrains.kotlin.codegen.state

import com.intellij.psi.PsiElement
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.codegen.ClassBuilderFactory
import org.jetbrains.kotlin.codegen.ClassBuilderMode
import org.jetbrains.kotlin.codegen.SignatureCollectingClassBuilderFactory
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.DELEGATION
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.FAKE_OVERRIDE
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.fileClasses.JvmFileClassesProvider
import org.jetbrains.kotlin.load.java.descriptors.SamAdapterDescriptor
import org.jetbrains.kotlin.load.java.descriptors.getParentJavaStaticClassScope
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.propertyIfAccessor
import org.jetbrains.kotlin.resolve.jvm.diagnostics.*
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*

private val EXTERNAL_SOURCES_KINDS = arrayOf(
        JvmDeclarationOriginKind.DELEGATION_TO_DEFAULT_IMPLS,
        JvmDeclarationOriginKind.DELEGATION,
        JvmDeclarationOriginKind.BRIDGE)

class BuilderFactoryForDuplicateSignatureDiagnostics(
        builderFactory: ClassBuilderFactory,
        bindingContext: BindingContext,
        private val diagnostics: DiagnosticSink,
        fileClassesProvider: JvmFileClassesProvider,
        incrementalCache: IncrementalCache?,
        moduleName: String
) : SignatureCollectingClassBuilderFactory(builderFactory) {

    // Avoid errors when some classes are not loaded for some reason
    private val typeMapper = JetTypeMapper(bindingContext, ClassBuilderMode.LIGHT_CLASSES, fileClassesProvider, incrementalCache,
                                           IncompatibleClassTracker.DoNothing, moduleName)
    private val reportDiagnosticsTasks = ArrayList<() -> Unit>()

    fun reportDiagnostics() {
        reportDiagnosticsTasks.forEach { it() }
        reportDiagnosticsTasks.clear()
    }

    override fun handleClashingSignatures(data: ConflictingJvmDeclarationsData) {
        reportDiagnosticsTasks.add { reportConflictingJvmSignatures(data) }
    }

    private fun reportConflictingJvmSignatures(data: ConflictingJvmDeclarationsData) {
        val noOwnImplementations = data.signatureOrigins.all { it.originKind in EXTERNAL_SOURCES_KINDS }

        val elements = LinkedHashSet<PsiElement>()
        if (noOwnImplementations) {
            elements.addIfNotNull(data.classOrigin.element)
        }
        else {
            for (origin in data.signatureOrigins) {
                var element = origin.element

                if (element == null || origin.originKind in EXTERNAL_SOURCES_KINDS) {
                    element = data.classOrigin.element
                }

                elements.addIfNotNull(element)
            }
        }

        for (element in elements) {
            diagnostics.report(ErrorsJvm.CONFLICTING_JVM_DECLARATIONS.on(element, data))
        }
    }

    override fun onClassDone(
            classOrigin: JvmDeclarationOrigin,
            classInternalName: String?,
            signatures: MultiMap<RawSignature, JvmDeclarationOrigin>
    ) {
        reportDiagnosticsTasks.add { reportClashingSignaturesInHierarchy(classOrigin, classInternalName, signatures) }
    }

    private fun reportClashingSignaturesInHierarchy(
            classOrigin: JvmDeclarationOrigin,
            classInternalName: String?,
            signatures: MultiMap<RawSignature, JvmDeclarationOrigin>
    ) {
        val descriptor = classOrigin.descriptor
        if (descriptor !is ClassDescriptor) return

        val groupedBySignature = groupMembersDescriptorsBySignature(descriptor)
        for ((rawSignature, origins) in signatures.entrySet()) {
            for (origin in origins) {
                if (origin.originKind == JvmDeclarationOriginKind.SYNTHETIC) {
                    groupedBySignature.putValue(rawSignature, origin)
                }
            }
        }

        signatures@
        for ((rawSignature, origins) in groupedBySignature.entrySet()) {
            if (origins.size <= 1) continue

            var memberElement: PsiElement? = null
            var ownNonFakeCount = 0
            for (origin in origins) {
                val member = origin.descriptor as? CallableMemberDescriptor?
                if (member != null && member.containingDeclaration == classOrigin.descriptor && member.kind != FAKE_OVERRIDE) {
                    ownNonFakeCount++
                    // If there's more than one real element, the clashing signature is already reported.
                    // Only clashes between fake overrides are interesting here
                    if (ownNonFakeCount > 1) continue@signatures

                    if (member.kind != DELEGATION) {
                        // Delegates don't have declarations in the code
                        memberElement = origin.element ?: DescriptorToSourceUtils.descriptorToDeclaration(member)
                        if (memberElement == null && member is PropertyAccessorDescriptor) {
                            memberElement = DescriptorToSourceUtils.descriptorToDeclaration(member.correspondingProperty)
                        }
                    }
                }
            }

            val elementToReportOn = memberElement ?: classOrigin.element
            if (elementToReportOn == null) return // TODO: it'd be better to report this error without any element at all

            val data = ConflictingJvmDeclarationsData(classInternalName, classOrigin, rawSignature, origins)
            if (memberElement != null) {
                diagnostics.report(ErrorsJvm.ACCIDENTAL_OVERRIDE.on(elementToReportOn, data))
            }
            else {
                diagnostics.report(ErrorsJvm.CONFLICTING_INHERITED_JVM_DECLARATIONS.on(elementToReportOn, data))
            }
        }
    }

    private fun groupMembersDescriptorsBySignature(descriptor: ClassDescriptor): MultiMap<RawSignature, JvmDeclarationOrigin> {
        val groupedBySignature = MultiMap.create<RawSignature, JvmDeclarationOrigin>()

        fun processMember(member: DeclarationDescriptor?) {
            if (member !is CallableMemberDescriptor) return
            // a member of super is not visible: no override
            if (member.visibility == Visibilities.INVISIBLE_FAKE) return
            // if a signature clashes with a SAM-adapter or something like that, there's no harm
            if (isOrOverridesSamAdapter(member)) return

            if (member is PropertyDescriptor) {
                processMember(member.getter)
                processMember(member.setter)
            }
            else if (member is FunctionDescriptor) {
                val signatures =
                        if (member.kind == FAKE_OVERRIDE)
                            member.overriddenDescriptors.mapTo(HashSet()) { it.original.asRawSignature() }
                        else
                            setOf(member.asRawSignature())

                signatures.forEach {
                    rawSignature ->
                    groupedBySignature.putValue(rawSignature, OtherOrigin(member))
                }
            }
        }

        descriptor.defaultType.memberScope.getContributedDescriptors().forEach(::processMember)
        descriptor.getParentJavaStaticClassScope()?.run {
            getContributedDescriptors(DescriptorKindFilter.FUNCTIONS)
                    .filter {
                        it is FunctionDescriptor && Visibilities.isVisibleWithIrrelevantReceiver(it, descriptor)
                    }
                    .forEach(::processMember)
        }

        return groupedBySignature
    }

    private fun FunctionDescriptor.asRawSignature() =
        with(typeMapper.mapSignature(this)) {
            RawSignature(asmMethod.name!!, asmMethod.descriptor!!, MemberKind.METHOD)
        }

    private fun isOrOverridesSamAdapter(descriptor: CallableMemberDescriptor): Boolean {
        if (descriptor is SamAdapterDescriptor<*>) return true

        return descriptor.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE
                && descriptor.overriddenDescriptors.all { isOrOverridesSamAdapter(it) }
    }
}
