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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.psi.*

public object AnnotationUseSiteTargetChecker {

    public fun check(annotated: KtAnnotated, descriptor: DeclarationDescriptor, trace: BindingTrace) {
        trace.checkDeclaration(annotated, descriptor)

        if (annotated is KtFunction) {
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
            val target = annotationWithTarget.target ?: continue
            fun annotationEntry() = DescriptorToSourceUtils.getSourceFromAnnotation(annotationWithTarget.annotation)

            when (target) {
                AnnotationUseSiteTarget.RECEIVER -> {}
                AnnotationUseSiteTarget.FIELD,
                AnnotationUseSiteTarget.PROPERTY,
                AnnotationUseSiteTarget.PROPERTY_GETTER,
                AnnotationUseSiteTarget.PROPERTY_SETTER,
                AnnotationUseSiteTarget.SETTER_PARAMETER -> {
                    annotationEntry()?.let { report(INAPPLICABLE_TARGET_ON_PROPERTY.on(it, target.renderName)) }
                }
                AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER -> annotationEntry()?.let { report(INAPPLICABLE_PARAM_TARGET.on(it)) }
                AnnotationUseSiteTarget.FILE -> throw IllegalArgumentException("@file annotations are not allowed here")
            }
        }
    }

    private fun BindingTrace.checkDeclaration(annotated: KtAnnotated, descriptor: DeclarationDescriptor) {
        for (annotation in annotated.annotationEntries) {
            val target = annotation.useSiteTarget?.getAnnotationUseSiteTarget() ?: continue

            when (target) {
                AnnotationUseSiteTarget.FIELD -> checkFieldTargetApplicability(annotated, annotation, descriptor)
                AnnotationUseSiteTarget.PROPERTY -> checkIfProperty(annotated, annotation)
                AnnotationUseSiteTarget.PROPERTY_GETTER -> checkIfProperty(annotated, annotation)
                AnnotationUseSiteTarget.PROPERTY_SETTER -> checkIfMutableProperty(annotated, annotation)
                AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER -> {
                    if (annotated !is KtParameter) {
                        report(INAPPLICABLE_PARAM_TARGET.on(annotation))
                    }
                    else {
                        val containingDeclaration = bindingContext[BindingContext.VALUE_PARAMETER, annotated]?.containingDeclaration
                        if (containingDeclaration !is ConstructorDescriptor || !containingDeclaration.isPrimary) {
                            report(INAPPLICABLE_PARAM_TARGET.on(annotation))
                        }
                        else if (!annotated.hasValOrVar()) {
                            report(REDUNDANT_ANNOTATION_TARGET.on(annotation, target.renderName))
                        }
                    }
                }
                AnnotationUseSiteTarget.SETTER_PARAMETER -> checkIfMutableProperty(annotated, annotation)
                AnnotationUseSiteTarget.FILE -> throw IllegalArgumentException("@file annotations are not allowed here")
                AnnotationUseSiteTarget.RECEIVER -> report(INAPPLICABLE_RECEIVER_TARGET.on(annotation))
            }
        }
    }

    private fun BindingTrace.checkFieldTargetApplicability(
            annotated: KtAnnotated,
            annotation: KtAnnotationEntry,
            descriptor: DeclarationDescriptor
    ) {
        if (!checkIfProperty(annotated, annotation)) return

        if (annotated is KtProperty && descriptor is PropertyDescriptor) {
            if (!annotated.hasDelegate() && !(bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, descriptor) ?: false)) {
                report(INAPPLICABLE_FIELD_TARGET_NO_BACKING_FIELD.on(annotation))
            }
        }
    }

    private fun BindingTrace.checkIfMutableProperty(annotated: KtAnnotated, annotation: KtAnnotationEntry) {
        if (!checkIfProperty(annotated, annotation)) return

        val isMutable = if (annotated is KtProperty)
            annotated.isVar
        else if (annotated is KtParameter)
            annotated.isMutable
        else false

        if (!isMutable) report(INAPPLICABLE_TARGET_PROPERTY_IMMUTABLE.on(annotation))
    }

    private fun BindingTrace.checkIfProperty(annotated: KtAnnotated, annotation: KtAnnotationEntry): Boolean {
        val isProperty = if (annotated is KtProperty)
            !annotated.isLocal
        else if (annotated is KtParameter)
            annotated.hasValOrVar()
        else false

        val target = annotation.useSiteTarget?.getAnnotationUseSiteTarget()?.renderName ?: "unknown target" // should not happen
        if (!isProperty) report(INAPPLICABLE_TARGET_ON_PROPERTY.on(annotation, target))
        return isProperty
    }
}