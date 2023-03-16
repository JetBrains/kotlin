/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.diagnostics.reportDiagnosticOnce
import org.jetbrains.kotlin.psi.*

object AnnotationUseSiteTargetChecker {

    fun check(
        annotated: KtAnnotated,
        descriptor: DeclarationDescriptor,
        trace: BindingTrace,
        languageVersionSettings: LanguageVersionSettings
    ) {
        trace.checkDeclaration(annotated, languageVersionSettings, descriptor)

        if (annotated is KtCallableDeclaration) {
            annotated.receiverTypeReference?.let { trace.checkTypeReference(it, languageVersionSettings, isReceiver = true) }
            annotated.typeReference?.let { trace.checkTypeReference(it, languageVersionSettings, isReceiver = false) }
        }

        if (annotated is KtFunction) {
            for (parameter in annotated.valueParameters) {
                if (parameter.hasValOrVar()) continue
                val parameterDescriptor = trace.bindingContext[BindingContext.VALUE_PARAMETER, parameter] ?: continue

                trace.checkDeclaration(parameter, languageVersionSettings, parameterDescriptor)
                parameter.typeReference?.let { trace.checkTypeReference(it, languageVersionSettings, isReceiver = false) }
            }
        }
    }

    private fun BindingTrace.checkTypeReference(
        topLevelTypeReference: KtTypeReference,
        languageVersionSettings: LanguageVersionSettings,
        isReceiver: Boolean
    ) {
        checkAsTopLevelTypeReference(topLevelTypeReference, languageVersionSettings, isReceiver)

        topLevelTypeReference.acceptChildren(
            typeReferenceRecursiveVisitor { typeReference ->
                checkAsTopLevelTypeReference(typeReference, languageVersionSettings, isReceiver = false)
            }
        )
    }

    private fun BindingTrace.checkAsTopLevelTypeReference(
        topLevelTypeReference: KtTypeReference,
        languageVersionSettings: LanguageVersionSettings,
        isReceiver: Boolean
    ) {
        for (annotationEntry in topLevelTypeReference.annotationEntries) {
            val target = annotationEntry.useSiteTarget?.getAnnotationUseSiteTarget() ?: continue

            if (target != AnnotationUseSiteTarget.RECEIVER || !isReceiver) {
                val diagnostic =
                    if (languageVersionSettings.supportsFeature(LanguageFeature.RestrictionOfWrongAnnotationsWithUseSiteTargetsOnTypes))
                        WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET.on(annotationEntry, "undefined target", target.renderName)
                    else
                        WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET_ON_TYPE.on(annotationEntry, target.renderName)

                reportDiagnosticOnce(diagnostic)
            }
        }
    }

    private fun BindingTrace.checkDeclaration(
        annotated: KtAnnotated,
        languageVersionSettings: LanguageVersionSettings,
        descriptor: DeclarationDescriptor
    ) {
        for (annotation in annotated.annotationEntries) {
            val useSiteTarget = annotation.useSiteTarget
            val target = useSiteTarget?.getAnnotationUseSiteTarget() ?: continue

            when (target) {
                AnnotationUseSiteTarget.FIELD -> checkIfHasBackingField(annotated, descriptor, annotation)
                AnnotationUseSiteTarget.PROPERTY,
                AnnotationUseSiteTarget.PROPERTY_GETTER -> checkIfProperty(
                    annotated,
                    annotation,
                    when (languageVersionSettings.supportsFeature(LanguageFeature.ProhibitUseSiteGetTargetAnnotations)) {
                        true -> INAPPLICABLE_TARGET_ON_PROPERTY
                        false -> INAPPLICABLE_TARGET_ON_PROPERTY_WARNING
                    }
                )
                AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD -> checkIfDelegatedProperty(annotated, annotation)
                AnnotationUseSiteTarget.PROPERTY_SETTER -> checkIfMutableProperty(annotated, annotation)
                AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER -> {
                    if (annotated !is KtParameter) {
                        report(INAPPLICABLE_PARAM_TARGET.on(annotation))
                    } else {
                        val containingDeclaration = bindingContext[BindingContext.VALUE_PARAMETER, annotated]?.containingDeclaration
                        if (containingDeclaration !is ConstructorDescriptor || !containingDeclaration.isPrimary) {
                            report(INAPPLICABLE_PARAM_TARGET.on(annotation))
                        } else if (!annotated.hasValOrVar()) {
                            report(REDUNDANT_ANNOTATION_TARGET.on(annotation, target.renderName))
                        }
                    }
                }
                AnnotationUseSiteTarget.SETTER_PARAMETER -> checkIfMutableProperty(annotated, annotation)
                AnnotationUseSiteTarget.FILE -> reportDiagnosticOnce(INAPPLICABLE_FILE_TARGET.on(useSiteTarget))
                AnnotationUseSiteTarget.RECEIVER ->
                    // annotation with use-site target `receiver` can be only on type reference, but not on declaration
                    reportDiagnosticOnce(WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET.on(annotation, "declaration", target.renderName))
            }
        }
    }

    private fun BindingTrace.checkIfDelegatedProperty(annotated: KtAnnotated, annotation: KtAnnotationEntry) {
        if (annotated is KtProperty && !annotated.hasDelegate() || annotated is KtParameter && annotated.hasValOrVar()) {
            report(INAPPLICABLE_TARGET_PROPERTY_HAS_NO_DELEGATE.on(annotation))
        }
    }

    private fun BindingTrace.checkIfHasBackingField(
        annotated: KtAnnotated,
        descriptor: DeclarationDescriptor,
        annotation: KtAnnotationEntry
    ) {
        if (annotated is KtProperty && annotated.hasDelegate() &&
            descriptor is PropertyDescriptor && get(BindingContext.BACKING_FIELD_REQUIRED, descriptor) != true) {
            report(INAPPLICABLE_TARGET_PROPERTY_HAS_NO_BACKING_FIELD.on(annotation))
        }
    }

    private fun KtAnnotationEntry.useSiteDescription() =
        useSiteTarget?.getAnnotationUseSiteTarget()?.renderName ?: "unknown target" // should not happen

    private fun BindingTrace.checkIfMutableProperty(annotated: KtAnnotated, annotation: KtAnnotationEntry) {
        if (!checkIfProperty(annotated, annotation, INAPPLICABLE_TARGET_ON_PROPERTY)) return

        val isMutable = when (annotated) {
            is KtProperty -> annotated.isVar
            is KtParameter -> annotated.isMutable
            else -> false
        }

        if (!isMutable) {
            report(INAPPLICABLE_TARGET_PROPERTY_IMMUTABLE.on(annotation, annotation.useSiteDescription()))
        }
    }

    private fun BindingTrace.checkIfProperty(
        annotated: KtAnnotated,
        annotation: KtAnnotationEntry,
        diagnosticFactory: DiagnosticFactory1<PsiElement, String>
    ): Boolean {
        val isProperty = when (annotated) {
            is KtProperty -> !annotated.isLocal
            is KtParameter -> annotated.hasValOrVar()
            else -> false
        }

        if (!isProperty) report(diagnosticFactory.on(annotation, annotation.useSiteDescription()))
        return isProperty
    }
}