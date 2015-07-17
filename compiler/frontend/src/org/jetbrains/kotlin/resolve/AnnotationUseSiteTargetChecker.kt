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

package org.jetbrains.kotlin.resolve

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.psi.*

public object AnnotationUseSiteTargetChecker {

    public fun check(annotated: JetAnnotated, descriptor: DeclarationDescriptor, trace: BindingTrace) {
        trace.checkDeclaration(annotated, descriptor)

        if (annotated is JetFunction) {
            for (parameter in annotated.valueParameters) {
                val parameterDescriptor = trace.bindingContext[BindingContext.VALUE_PARAMETER, parameter] ?: continue
                trace.checkDeclaration(parameter, parameterDescriptor)
            }
        }

    }

    private fun BindingTrace.checkDeclaration(annotated: JetAnnotated, descriptor: DeclarationDescriptor) {
        for (annotationWithTarget in descriptor.annotations.getUseSiteTargetedAnnotations()) {
            val annotation = annotationWithTarget.annotation
            val target = annotationWithTarget.target ?: continue

            when (target) {
                AnnotationUseSiteTarget.FIELD -> checkFieldTargetApplicability(annotated, descriptor, annotation)
                AnnotationUseSiteTarget.PROPERTY -> checkIfPropertyDescriptor(descriptor, annotation, INAPPLICABLE_PROPERTY_TARGET)
                AnnotationUseSiteTarget.PROPERTY_GETTER -> checkIfPropertyDescriptor(descriptor, annotation, INAPPLICABLE_GET_TARGET)
                AnnotationUseSiteTarget.PROPERTY_SETTER -> checkMutableProperty(descriptor, annotation, INAPPLICABLE_SET_TARGET)
                AnnotationUseSiteTarget.RECEIVER -> {
                    if (descriptor !is FunctionDescriptor && descriptor !is PropertyDescriptor) {
                        report(annotation, INAPPLICABLE_RECEIVER_TARGET)
                    }
                    else if ((descriptor as CallableMemberDescriptor).extensionReceiverParameter == null) {
                        report(annotation, INAPPLICABLE_RECEIVER_TARGET)
                    }
                }
                AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER -> {
                    if (annotated !is JetParameter) {
                        report(annotation, INAPPLICABLE_PARAM_TARGET)
                    }
                    else {
                        val containingDeclaration = bindingContext[BindingContext.VALUE_PARAMETER, annotated]?.containingDeclaration
                        if (containingDeclaration !is ConstructorDescriptor || !containingDeclaration.isPrimary) {
                            report(annotation, INAPPLICABLE_PARAM_TARGET)
                        }
                    }
                }
                AnnotationUseSiteTarget.SETTER_PARAMETER -> checkMutableProperty(descriptor, annotation, INAPPLICABLE_SPARAM_TARGET)
                AnnotationUseSiteTarget.FILE -> throw IllegalArgumentException("@file annotations are not allowed here")
            }
        }
    }

    private fun BindingTrace.checkFieldTargetApplicability(
            modifierListOwner: JetAnnotated,
            descriptor: DeclarationDescriptor,
            annotation: AnnotationDescriptor) {
        if (checkIfPropertyDescriptor(descriptor, annotation, INAPPLICABLE_FIELD_TARGET)) return

        descriptor as PropertyDescriptor
        val hasDelegate = modifierListOwner is JetProperty && modifierListOwner.hasDelegate()

        if (!hasDelegate && !(bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, descriptor) ?: false)) {
            report(annotation, INAPPLICABLE_FIELD_TARGET_NO_BACKING_FIELD)
        }
    }

    private fun BindingTrace.checkMutableProperty(
            descriptor: DeclarationDescriptor,
            annotation: AnnotationDescriptor,
            diagnosticFactory: DiagnosticFactory0<PsiElement>) {
        checkIfPropertyDescriptor(descriptor, annotation, diagnosticFactory)

        if (descriptor is PropertyDescriptor) {
            if (!descriptor.isVar) {
                report(annotation, INAPPLICABLE_TARGET_PROPERTY_IMMUTABLE)
            }
        }
    }

    private fun BindingTrace.checkIfPropertyDescriptor(
            descriptor: DeclarationDescriptor,
            annotation: AnnotationDescriptor,
            diagnosticFactory: DiagnosticFactory0<PsiElement>): Boolean {
        if (descriptor !is PropertyDescriptor) {
            report(annotation, diagnosticFactory)
            return true
        }
        return false
    }

    private fun BindingTrace.report(annotation: AnnotationDescriptor, diagnosticFactory: DiagnosticFactory0<PsiElement>) {
        val annotationEntry = DescriptorToSourceUtils.getSourceFromAnnotation(annotation) ?: return
        report(diagnosticFactory.on(annotationEntry))
    }

}