/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.annotations.KotlinRetention
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.resolve.AdditionalAnnotationChecker
import org.jetbrains.kotlin.resolve.AnnotationChecker
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getAnnotationRetention
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class ExperimentalMarkerDeclarationAnnotationChecker(private val module: ModuleDescriptor) : AdditionalAnnotationChecker {
    override fun checkEntries(
        entries: List<KtAnnotationEntry>,
        actualTargets: List<KotlinTarget>,
        trace: BindingTrace,
        annotated: KtAnnotated?,
        languageVersionSettings: LanguageVersionSettings
    ) {
        var isAnnotatedWithExperimental = false

        for (entry in entries) {
            val annotation = trace.bindingContext.get(BindingContext.ANNOTATION, entry) ?: continue
            when (annotation.fqName) {
                in OptInNames.OPT_IN_FQ_NAMES -> {
                    val annotationClasses =
                        annotation.allValueArguments[OptInNames.USE_EXPERIMENTAL_ANNOTATION_CLASS]
                            .safeAs<ArrayValue>()?.value.orEmpty()
                    checkOptInUsage(annotationClasses, trace, entry)
                }
                in OptInNames.REQUIRES_OPT_IN_FQ_NAMES -> {
                    isAnnotatedWithExperimental = true
                }
            }
            val annotationClass = annotation.annotationClass ?: continue
            if (annotationClass.annotations.any { it.fqName in OptInNames.REQUIRES_OPT_IN_FQ_NAMES }) {
                val applicableTargets = AnnotationChecker.applicableTargetSet(annotationClass)
                val possibleTargets = applicableTargets.intersect(actualTargets)
                val annotationUseSiteTarget = entry.useSiteTarget?.getAnnotationUseSiteTarget()
                if (PROPERTY_GETTER in possibleTargets ||
                    annotationUseSiteTarget == AnnotationUseSiteTarget.PROPERTY_GETTER
                ) {
                    trace.report(Errors.OPT_IN_MARKER_ON_WRONG_TARGET.on(entry, "getter"))
                }
                if (VALUE_PARAMETER in possibleTargets && annotationUseSiteTarget == null ||
                    annotationUseSiteTarget == AnnotationUseSiteTarget.RECEIVER ||
                    annotationUseSiteTarget == AnnotationUseSiteTarget.SETTER_PARAMETER ||
                    annotationUseSiteTarget == AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER
                ) {
                    trace.report(Errors.OPT_IN_MARKER_ON_WRONG_TARGET.on(entry, "parameter"))
                }
                if (LOCAL_VARIABLE in possibleTargets) {
                    trace.report(Errors.OPT_IN_MARKER_ON_WRONG_TARGET.on(entry, "variable"))
                }
                if (annotationUseSiteTarget == AnnotationUseSiteTarget.FIELD ||
                    annotationUseSiteTarget == AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD
                ) {
                    trace.report(Errors.OPT_IN_MARKER_ON_WRONG_TARGET.on(entry, "field"))
                }
            }
        }

        if (isAnnotatedWithExperimental) {
            checkMarkerTargetsAndRetention(entries, trace)
        }
    }

    private fun CallableMemberDescriptor.hasExperimentalOverriddenDescriptors(
        experimentalFqName: FqName,
        visited: MutableSet<CallableMemberDescriptor> = mutableSetOf()
    ): Boolean {
        if (!visited.add(this)) return false
        for (overridden in overriddenDescriptors) {
            if (overridden.annotations.any { it.fqName == experimentalFqName }) {
                return true
            }
            var containingDeclaration = overridden.containingDeclaration
            while (containingDeclaration is ClassDescriptor) {
                if (containingDeclaration.annotations.any { it.fqName == experimentalFqName }) {
                    return true
                }
                containingDeclaration = containingDeclaration.containingDeclaration
            }
            if (overridden.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE &&
                overridden.hasExperimentalOverriddenDescriptors(experimentalFqName)
            ) {
                return true
            }
        }
        return false
    }

    private fun checkOptInUsage(annotationClasses: List<ConstantValue<*>>, trace: BindingTrace, entry: KtAnnotationEntry) {
        if (annotationClasses.isEmpty()) {
            trace.report(Errors.OPT_IN_WITHOUT_ARGUMENTS.on(entry))
            return
        }

        for (annotationClass in annotationClasses) {
            val classDescriptor =
                (annotationClass as? KClassValue)?.getArgumentType(module)?.constructor?.declarationDescriptor as? ClassDescriptor
                    ?: continue
            val experimentality = with(ExperimentalUsageChecker) {
                classDescriptor.loadExperimentalityForMarkerAnnotation()
            }
            if (experimentality == null) {
                trace.report(Errors.OPT_IN_ARGUMENT_IS_NOT_MARKER.on(entry, classDescriptor.fqNameSafe))
            }
        }
    }

    private fun checkMarkerTargetsAndRetention(
        entries: List<KtAnnotationEntry>,
        trace: BindingTrace
    ) {
        val associatedEntries = entries.associateWith { entry -> trace.bindingContext.get(BindingContext.ANNOTATION, entry) }.entries
        val targetEntry = associatedEntries.firstOrNull { (_, descriptor) ->
            descriptor?.fqName == StandardNames.FqNames.target
        }
        if (targetEntry != null) {
            val (entry, descriptor) = targetEntry
            val allowedTargets = AnnotationChecker.loadAnnotationTargets(descriptor!!) ?: return
            val wrongTargets = allowedTargets.intersect(Experimentality.WRONG_TARGETS_FOR_MARKER)
            if (wrongTargets.isNotEmpty()) {
                trace.report(
                    Errors.OPT_IN_MARKER_WITH_WRONG_TARGET.on(
                        entry,
                        wrongTargets.joinToString(transform = KotlinTarget::description)
                    )
                )
            }
        }
        val retentionEntry = associatedEntries.firstOrNull { (_, descriptor) ->
            descriptor?.fqName == StandardNames.FqNames.retention
        }
        if (retentionEntry != null) {
            val (entry, descriptor) = retentionEntry
            if (descriptor?.getAnnotationRetention() == KotlinRetention.SOURCE) {
                trace.report(Errors.OPT_IN_MARKER_WITH_WRONG_RETENTION.on(entry))
            }
        }
    }
}
