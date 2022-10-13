/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.annotations.KotlinRetention
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.*
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

class OptInMarkerDeclarationAnnotationChecker(private val module: ModuleDescriptor) : AdditionalAnnotationChecker {
    override fun checkEntries(
        entries: List<KtAnnotationEntry>,
        actualTargets: List<KotlinTarget>,
        trace: BindingTrace,
        annotated: KtAnnotated?,
        languageVersionSettings: LanguageVersionSettings
    ) {
        var hasOptIn = false

        for (entry in entries) {
            val annotation = trace.bindingContext.get(BindingContext.ANNOTATION, entry) ?: continue
            when (annotation.fqName) {
                OptInNames.OPT_IN_FQ_NAME -> {
                    val annotationClasses = (annotation.allValueArguments[OptInNames.OPT_IN_ANNOTATION_CLASS] as? ArrayValue)
                        ?.value.orEmpty()
                    checkOptInUsage(annotationClasses, trace, entry)
                }
                OptInNames.SUBCLASS_OPT_IN_REQUIRED_FQ_NAME -> {
                    val annotationClass =
                        annotation.allValueArguments[OptInNames.OPT_IN_ANNOTATION_CLASS]
                    checkSubclassOptInUsage(annotated, listOfNotNull(annotationClass), trace, entry)
                }
                OptInNames.REQUIRES_OPT_IN_FQ_NAME -> {
                    hasOptIn = true
                }
            }
            val annotationClass = annotation.annotationClass ?: continue
            if (annotationClass.annotations.any { it.fqName == OptInNames.REQUIRES_OPT_IN_FQ_NAME }) {
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

        if (hasOptIn) {
            checkMarkerTargetsAndRetention(entries, trace)
        }
    }

    private fun checkOptInUsage(annotationClasses: List<ConstantValue<*>>, trace: BindingTrace, entry: KtAnnotationEntry) {
        if (annotationClasses.isEmpty()) {
            trace.report(Errors.OPT_IN_WITHOUT_ARGUMENTS.on(entry))
            return
        }
        checkArgumentsAreMarkers(annotationClasses, trace, entry)
    }

    private fun checkSubclassOptInUsage(
        annotated: KtAnnotated?,
        annotationClasses: List<ConstantValue<*>>,
        trace: BindingTrace,
        entry: KtAnnotationEntry
    ) {
        when (annotated) {
            is KtAnnotatedExpression -> {
                if (annotated.baseExpression is KtObjectLiteralExpression) {
                    trace.report(Errors.SUBCLASS_OPT_IN_INAPPLICABLE.on(entry, "object"))
                }
                return
            }
            is KtClassOrObject -> {
                val descriptor = trace[BindingContext.CLASS, annotated]
                if (descriptor != null) {
                    val kind = descriptor.kind
                    if (kind == ClassKind.OBJECT || kind == ClassKind.ENUM_CLASS || kind == ClassKind.ANNOTATION_CLASS) {
                        trace.report(Errors.SUBCLASS_OPT_IN_INAPPLICABLE.on(entry, kind.toString()))
                        return
                    }
                    if (kind != ClassKind.ENUM_ENTRY) {
                        // ^ We don't report anything on enum entries because it's anyway inapplicable target
                        val modality = descriptor.modality
                        if (modality != Modality.ABSTRACT && modality != Modality.OPEN) {
                            trace.report(Errors.SUBCLASS_OPT_IN_INAPPLICABLE.on(entry, "$modality $kind"))
                            return
                        }
                        if (descriptor.isFun) {
                            trace.report(Errors.SUBCLASS_OPT_IN_INAPPLICABLE.on(entry, "fun interface"))
                            return
                        }
                        if (annotated.isLocal) {
                            trace.report(Errors.SUBCLASS_OPT_IN_INAPPLICABLE.on(entry, "local $kind"))
                            return
                        }
                    }
                }
            }
        }
        checkArgumentsAreMarkers(annotationClasses, trace, entry)
    }

    private fun checkArgumentsAreMarkers(annotationClasses: List<ConstantValue<*>>, trace: BindingTrace, entry: KtAnnotationEntry) {
        for (annotationClass in annotationClasses) {
            val classDescriptor =
                (annotationClass as? KClassValue)?.getArgumentType(module)?.constructor?.declarationDescriptor as? ClassDescriptor
                    ?: continue
            val optInDescription = with(OptInUsageChecker) {
                classDescriptor.loadOptInForMarkerAnnotation()
            }
            if (optInDescription == null) {
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
            val wrongTargets = allowedTargets.intersect(OptInDescription.WRONG_TARGETS_FOR_MARKER)
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

@Deprecated("Please use OptInMarkerDeclarationAnnotationChecker instead", ReplaceWith("OptInMarkerDeclarationAnnotationChecker"))
@Suppress("unused")
typealias ExperimentalMarkerDeclarationAnnotationChecker = OptInMarkerDeclarationAnnotationChecker
