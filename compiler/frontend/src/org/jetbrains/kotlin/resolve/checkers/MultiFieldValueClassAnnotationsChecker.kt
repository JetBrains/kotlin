/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.calls.util.getType
import org.jetbrains.kotlin.resolve.needsMfvcFlattening
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.KotlinType

object MultiFieldValueClassAnnotationsChecker : DeclarationChecker {
    private fun report(context: DeclarationCheckerContext, name: String, type: KotlinType, annotationEntry: KtAnnotationEntry) {
        if (!type.needsMfvcFlattening()) return
        context.trace.report(Errors.ANNOTATION_ON_ILLEGAL_MULTI_FIELD_VALUE_CLASS_TYPED_TARGET.on(annotationEntry, name))
    }

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        fun report(name: String, type: KotlinType, annotations: Annotations) {
            for (annotationDescriptor in annotations) {
                report(context, name, type, annotationDescriptor.source.getPsi() as KtAnnotationEntry)
            }
        }

        when (descriptor) {
            is PropertyDescriptor -> {
                descriptor.backingField?.let { report("fields", descriptor.type, it.annotations) }
                val delegateType = (declaration as? KtProperty)?.delegateExpression?.getType(context.trace.bindingContext)
                descriptor.delegateField?.let {
                    if (delegateType == null) return@let
                    report("delegate fields", delegateType, it.annotations)
                }
                descriptor.getter?.let { getterDescriptor ->
                    if (getterDescriptor.contextReceiverParameters.isNotEmpty() || getterDescriptor.extensionReceiverParameter != null) return@let
                    val type = getterDescriptor.returnType ?: return@let
                    report("getters", type, getterDescriptor.annotations)
                }
                descriptor.setter?.valueParameters?.single()?.let { report("parameters", it.type, it.annotations) }
                descriptor.extensionReceiverParameter?.let { report("receivers", it.type, it.annotations) }
                descriptor.contextReceiverParameters.forEach { report("receivers", it.type, it.annotations) }
            }
            is PropertyAccessorDescriptor -> Unit
            is LocalVariableDescriptor -> {
                report("variables", descriptor.type, descriptor.annotations)
            }
            is CallableDescriptor -> {
                descriptor.extensionReceiverParameter?.let { report("receivers", it.type, it.annotations) }
                descriptor.contextReceiverParameters.forEach { report("receivers", it.type, it.annotations) }
                descriptor.valueParameters.forEach { report("parameters", it.type, it.annotations) }
            }
        }
    }
}
