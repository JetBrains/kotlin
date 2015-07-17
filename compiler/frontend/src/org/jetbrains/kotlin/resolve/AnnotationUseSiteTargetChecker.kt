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
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0
import org.jetbrains.kotlin.psi.JetDeclaration
import org.jetbrains.kotlin.psi.JetProperty
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.psi.JetAnnotationEntry

public object AnnotationUseSiteTargetChecker {

    public fun BindingTrace.check(modifierListOwner: JetDeclaration, descriptor: DeclarationDescriptor) {
        for (annotationWithTarget in descriptor.getAnnotations().getUseSiteTargetedAnnotations()) {
            val annotation = annotationWithTarget.annotation
            val target = annotationWithTarget.target ?: continue

            when (target) {
                AnnotationUseSiteTarget.FIELD -> checkFieldTargetApplicability(modifierListOwner, descriptor, annotation)
                AnnotationUseSiteTarget.PROPERTY -> checkIfPropertyDescriptor(descriptor, annotation, INAPPLICABLE_PROPERTY_TARGET)
                AnnotationUseSiteTarget.PROPERTY_GETTER -> checkIfPropertyDescriptor(descriptor, annotation, INAPPLICABLE_GET_TARGET)
                AnnotationUseSiteTarget.PROPERTY_SETTER -> checkMutableProperty(descriptor, annotation, INAPPLICABLE_SET_TARGET)
                AnnotationUseSiteTarget.RECEIVER -> {
                    if (descriptor !is FunctionDescriptor && descriptor !is PropertyDescriptor) {
                        report(annotation, INAPPLICABLE_RECEIVER_TARGET)
                    }
                    else if ((descriptor as CallableMemberDescriptor).getExtensionReceiverParameter() == null) {
                        report(annotation, INAPPLICABLE_RECEIVER_TARGET)
                    }
                }
                AnnotationUseSiteTarget.SETTER_PARAMETER -> checkMutableProperty(descriptor, annotation, INAPPLICABLE_SPARAM_TARGET)
                AnnotationUseSiteTarget.FILE -> throw IllegalArgumentException("@file annotations are not allowed here")
            }
        }
    }

    private fun BindingTrace.checkFieldTargetApplicability(
            modifierListOwner: JetDeclaration,
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
            if (!descriptor.isVar()) {
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
        val annotationEntry = get(BindingContext.ANNOTATION_DESCRIPTOR_TO_PSI_ELEMENT, annotation) ?: return
        report(diagnosticFactory.on(annotationEntry))
    }

}