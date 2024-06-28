/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.state

import com.intellij.psi.PsiElement
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.codegen.ClassBuilderFactory
import org.jetbrains.kotlin.codegen.ClassBuilderMode
import org.jetbrains.kotlin.codegen.SignatureCollectingClassBuilderFactory
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.DELEGATION
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.FAKE_OVERRIDE
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.load.java.descriptors.getParentJavaStaticClassScope
import org.jetbrains.kotlin.load.java.sam.SamAdapterDescriptor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.overriddenTreeUniqueAsSequence
import org.jetbrains.kotlin.resolve.jvm.diagnostics.*
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.org.objectweb.asm.commons.Method

private val EXTERNAL_SOURCES_KINDS = arrayOf(
    JvmDeclarationOriginKind.CLASS_MEMBER_DELEGATION_TO_DEFAULT_IMPL,
    JvmDeclarationOriginKind.DEFAULT_IMPL_DELEGATION_TO_SUPERINTERFACE_DEFAULT_IMPL,
    JvmDeclarationOriginKind.DELEGATION,
    JvmDeclarationOriginKind.BRIDGE
)

private val PREDEFINED_SIGNATURES = listOf(
    "getClass()Ljava/lang/Class;",
    "notify()V",
    "notifyAll()V",
    "wait()V",
    "wait(J)V",
    "wait(JI)V"
).map { signature ->
    RawSignature(signature.substringBefore('('), signature.substring(signature.indexOf('(')), MemberKind.METHOD)
}

class BuilderFactoryForDuplicateSignatureDiagnostics(
    builderFactory: ClassBuilderFactory,
    bindingContext: BindingContext,
    private val diagnostics: DiagnosticSink,
    moduleName: String,
    val languageVersionSettings: LanguageVersionSettings,
    useOldInlineClassesManglingScheme: Boolean,
    shouldGenerate: (JvmDeclarationOrigin) -> Boolean,
) : SignatureCollectingClassBuilderFactory(builderFactory, shouldGenerate) {

    private val mapAsmMethod: (FunctionDescriptor) -> Method = KotlinTypeMapper(
        // Avoid errors when some classes are not loaded for some reason
        bindingContext, ClassBuilderMode.LIGHT_CLASSES, moduleName, languageVersionSettings, isIrBackend = false,
        useOldInlineClassesManglingScheme = useOldInlineClassesManglingScheme
    )::mapAsmMethod

    private val reportDiagnosticsTasks = ArrayList<() -> Unit>()

    fun reportDiagnostics() {
        reportDiagnosticsTasks.forEach { it() }
        reportDiagnosticsTasks.clear()
    }

    override fun handleClashingSignatures(data: ConflictingJvmDeclarationsData) {
        reportDiagnosticsTasks.add { reportConflictingJvmSignatures(data) }
    }

    private fun reportConflictingJvmSignatures(data: ConflictingJvmDeclarationsData) {
        val noOwnImplementations = data.signatureOrigins!!.all { it.originKind in EXTERNAL_SOURCES_KINDS }

        val elements = LinkedHashSet<PsiElement>()
        if (noOwnImplementations) {
            elements.addIfNotNull(data.classOrigin!!.element)
        } else {
            for (origin in data.signatureOrigins!!) {
                var element = origin.element

                if (element == null || origin.originKind in EXTERNAL_SOURCES_KINDS) {
                    element = data.classOrigin!!.element
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
        classInternalName: String,
        signatures: MultiMap<RawSignature, JvmDeclarationOrigin>
    ) {
        reportDiagnosticsTasks.add {
            reportClashingWithPredefinedSignatures(classOrigin, classInternalName, signatures)
            reportClashingSignaturesInHierarchy(classOrigin, classInternalName, signatures)
        }
    }

    private fun reportClashingWithPredefinedSignatures(
        classOrigin: JvmDeclarationOrigin,
        classInternalName: String,
        signatures: MultiMap<RawSignature, JvmDeclarationOrigin>
    ) {
        for (predefinedSignature in PREDEFINED_SIGNATURES) {
            val signature = signatures[predefinedSignature]
            if (signature.isEmpty()) continue

            val origins = signature + JvmDeclarationOrigin.NO_ORIGIN

            val diagnostic = computeDiagnosticToReport(classOrigin, classInternalName, predefinedSignature, origins) ?: continue
            diagnostics.report(ErrorsJvm.CONFLICTING_INHERITED_JVM_DECLARATIONS.on(diagnostic.element, diagnostic.data))
        }
    }

    private fun reportClashingSignaturesInHierarchy(
        classOrigin: JvmDeclarationOrigin,
        classInternalName: String,
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

        for ((rawSignature, origins) in groupedBySignature.entrySet()) {
            if (origins.size <= 1) continue

            when (val diagnostic = computeDiagnosticToReport(classOrigin, classInternalName, rawSignature, origins)) {
                is ConflictingDeclarationError.AccidentalOverride -> {
                    diagnostics.report(ErrorsJvm.ACCIDENTAL_OVERRIDE.on(diagnostic.element, diagnostic.data))
                }
                is ConflictingDeclarationError.ConflictingInheritedJvmDeclarations -> {
                    diagnostics.report(ErrorsJvm.CONFLICTING_INHERITED_JVM_DECLARATIONS.on(diagnostic.element, diagnostic.data))
                }
                null -> {}
            }
        }
    }

    private sealed class ConflictingDeclarationError(val element: PsiElement, val data: ConflictingJvmDeclarationsData) {
        class AccidentalOverride(element: PsiElement, data: ConflictingJvmDeclarationsData) :
            ConflictingDeclarationError(element, data)

        class ConflictingInheritedJvmDeclarations(element: PsiElement, data: ConflictingJvmDeclarationsData) :
            ConflictingDeclarationError(element, data)
    }

    private fun computeDiagnosticToReport(
        classOrigin: JvmDeclarationOrigin,
        classInternalName: String,
        rawSignature: RawSignature,
        origins: Collection<JvmDeclarationOrigin>
    ): ConflictingDeclarationError? {
        var memberElement: PsiElement? = null
        var ownNonFakeCount = 0
        for (origin in origins) {
            val member = origin.descriptor as? CallableMemberDescriptor?
            if (member != null && member.containingDeclaration == classOrigin.descriptor && member.kind != FAKE_OVERRIDE) {
                ownNonFakeCount++
                // If there's more than one real element, the clashing signature is already reported.
                // Only clashes between fake overrides are interesting here
                if (ownNonFakeCount > 1) return null

                if (member.kind != DELEGATION) {
                    // Delegates don't have declarations in the code
                    memberElement = origin.element ?: DescriptorToSourceUtils.descriptorToDeclaration(member)
                    if (memberElement == null && member is PropertyAccessorDescriptor) {
                        memberElement = DescriptorToSourceUtils.descriptorToDeclaration(member.correspondingProperty)
                    }
                }
            }
        }

        val data = ConflictingJvmDeclarationsData(
            classInternalName, classOrigin, rawSignature, origins, origins.mapNotNull(JvmDeclarationOrigin::descriptor),
        )
        if (memberElement != null) {
            return ConflictingDeclarationError.AccidentalOverride(memberElement, data)
        }

        // TODO: it'd be better to report this error without any element at all
        val elementToReportOn = classOrigin.element ?: return null

        return ConflictingDeclarationError.ConflictingInheritedJvmDeclarations(elementToReportOn, data)
    }

    private fun groupMembersDescriptorsBySignature(descriptor: ClassDescriptor): MultiMap<RawSignature, JvmDeclarationOrigin> {
        val groupedBySignature = MultiMap.create<RawSignature, JvmDeclarationOrigin>()

        fun processMember(member: DeclarationDescriptor?) {
            if (member !is CallableMemberDescriptor) return
            // a member of super is not visible: no override
            if (member.visibility == DescriptorVisibilities.INVISIBLE_FAKE) return
            // if a signature clashes with a SAM-adapter or something like that, there's no harm
            if (isOrOverridesSamAdapter(member)) return

            if (member is PropertyDescriptor) {
                processMember(member.getter)
                processMember(member.setter)
            } else if (member is FunctionDescriptor) {
                val signatures =
                    if (member.kind == FAKE_OVERRIDE)
                        member.overriddenTreeUniqueAsSequence(useOriginal = true)
                            // drop the root (itself)
                            .drop(1)
                            .mapTo(HashSet()) { it.asRawSignature() }
                    else
                        setOf(member.asRawSignature())

                signatures.forEach { rawSignature ->
                    groupedBySignature.putValue(rawSignature, OtherOrigin(member))
                }
            }
        }

        descriptor.defaultType.memberScope.getContributedDescriptors().forEach(::processMember)
        descriptor.getParentJavaStaticClassScope()?.run {
            getContributedDescriptors(DescriptorKindFilter.FUNCTIONS)
                .filter {
                    it is FunctionDescriptor && DescriptorVisibilityUtils.isVisibleIgnoringReceiver(it, descriptor, languageVersionSettings)
                }
                .forEach(::processMember)
        }

        return groupedBySignature
    }

    private fun FunctionDescriptor.asRawSignature() =
        with(mapAsmMethod(this)) {
            RawSignature(name, descriptor, MemberKind.METHOD)
        }

    private fun isOrOverridesSamAdapter(descriptor: CallableMemberDescriptor): Boolean {
        if (descriptor is SamAdapterDescriptor<*>) return true

        return descriptor.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE
                && descriptor.overriddenDescriptors.all { isOrOverridesSamAdapter(it) }
    }
}
