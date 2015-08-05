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
import org.jetbrains.kotlin.descriptors.annotations.AnnotationWithTarget
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1
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

        if (descriptor is CallableDescriptor) trace.checkReceiverAnnotations(descriptor)
    }

    private fun BindingTrace.checkReceiverAnnotations(descriptor: CallableDescriptor) {
        val extensionReceiver = descriptor.extensionReceiverParameter ?: return
        for (annotationWithTarget in extensionReceiver.type.annotations.getUseSiteTargetedAnnotations()) {
            val annotation = annotationWithTarget.annotation
            val target = annotationWithTarget.target ?: continue

            when (target) {
                AnnotationUseSiteTarget.RECEIVER -> {}
                AnnotationUseSiteTarget.FIELD,
                    AnnotationUseSiteTarget.PROPERTY,
                    AnnotationUseSiteTarget.PROPERTY_GETTER,
                    AnnotationUseSiteTarget.PROPERTY_SETTER,
                    AnnotationUseSiteTarget.SETTER_PARAMETER -> report(annotationWithTarget, INAPPLICABLE_TARGET_ON_PROPERTY)
                AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER -> report(annotation, INAPPLICABLE_PARAM_TARGET)
                AnnotationUseSiteTarget.FILE -> throw IllegalArgumentException("@file annotations are not allowed here")
            }
        }
    }

    private fun BindingTrace.checkDeclaration(annotated: JetAnnotated, descriptor: DeclarationDescriptor) {
        for (annotationWithTarget in descriptor.annotations.getUseSiteTargetedAnnotations()) {
            val annotation = annotationWithTarget.annotation
            val target = annotationWithTarget.target ?: continue

            when (target) {
                AnnotationUseSiteTarget.FIELD -> checkFieldTargetApplicability(annotated, descriptor, annotationWithTarget)
                AnnotationUseSiteTarget.PROPERTY -> checkIfPropertyDescriptor(descriptor, annotationWithTarget)
                AnnotationUseSiteTarget.PROPERTY_GETTER -> checkIfPropertyDescriptor(descriptor, annotationWithTarget)
                AnnotationUseSiteTarget.PROPERTY_SETTER -> checkMutableProperty(descriptor, annotationWithTarget)
                AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER -> {
                    if (annotated !is JetParameter) {
                        report(annotation, INAPPLICABLE_PARAM_TARGET)
                    }
                    else {
                        val containingDeclaration = bindingContext[BindingContext.VALUE_PARAMETER, annotated]?.containingDeclaration
                        if (containingDeclaration !is ConstructorDescriptor || !containingDeclaration.isPrimary) {
                            report(annotation, INAPPLICABLE_PARAM_TARGET)
                        }
                        else if (!annotated.hasValOrVar()) {
                            report(annotationWithTarget, REDUNDANT_ANNOTATION_TARGET)
                        }
                    }
                }
                AnnotationUseSiteTarget.SETTER_PARAMETER -> checkMutableProperty(descriptor, annotationWithTarget)
                AnnotationUseSiteTarget.FILE -> throw IllegalArgumentException("@file annotations are not allowed here")
                AnnotationUseSiteTarget.RECEIVER -> report(annotation, INAPPLICABLE_RECEIVER_TARGET)
            }
        }
    }

    private fun BindingTrace.checkFieldTargetApplicability(
            modifierListOwner: JetAnnotated,
            descriptor: DeclarationDescriptor,
            annotationWithTarget: AnnotationWithTarget) {
        val propertyDescriptor = checkIfPropertyDescriptor(descriptor, annotationWithTarget) ?: return

        val hasDelegate = modifierListOwner is JetProperty && modifierListOwner.hasDelegate()

        if (!hasDelegate && !(bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, propertyDescriptor) ?: false)) {
            report(annotationWithTarget.annotation, INAPPLICABLE_FIELD_TARGET_NO_BACKING_FIELD)
        }
    }

    private fun BindingTrace.checkMutableProperty(descriptor: DeclarationDescriptor, annotationWIthTarget: AnnotationWithTarget) {
        val propertyDescriptor = checkIfPropertyDescriptor(descriptor, annotationWIthTarget) ?: return

        if (!propertyDescriptor.isVar) {
            report(annotationWIthTarget.annotation, INAPPLICABLE_TARGET_PROPERTY_IMMUTABLE)
        }
    }

    private fun BindingTrace.checkIfPropertyDescriptor(
            descriptor: DeclarationDescriptor,
            annotationWithTarget: AnnotationWithTarget): PropertyDescriptor? {
        if (descriptor is PropertyDescriptor) {
            return descriptor
        }
        else if (descriptor is ValueParameterDescriptor) {
            val jetParameter = DescriptorToSourceUtils.descriptorToDeclaration(descriptor) as? JetParameter
            if (jetParameter != null && jetParameter.hasValOrVar()) {
                val propertyDescriptor = bindingContext[BindingContext.VALUE_PARAMETER_AS_PROPERTY, descriptor]
                if (propertyDescriptor != null) return propertyDescriptor
            }
        }

        report(annotationWithTarget, INAPPLICABLE_TARGET_ON_PROPERTY)
        return null
    }

    private fun BindingTrace.report(annotation: AnnotationDescriptor, diagnosticFactory: DiagnosticFactory0<PsiElement>) {
        val annotationEntry = DescriptorToSourceUtils.getSourceFromAnnotation(annotation) ?: return
        report(diagnosticFactory.on(annotationEntry))
    }

    private fun BindingTrace.report(annotationWithTarget: AnnotationWithTarget, diagnosticFactory: DiagnosticFactory1<PsiElement, String>) {
        val annotationEntry = DescriptorToSourceUtils.getSourceFromAnnotation(annotationWithTarget.annotation) ?: return
        report(diagnosticFactory.on(annotationEntry, annotationWithTarget.target?.renderName ?: "invalid target"))
    }


}