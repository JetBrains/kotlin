/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.impl.SyntheticFieldDescriptor
import org.jetbrains.kotlin.idea.highlighter.KotlinHighlightingColors.*
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.tasks.isDynamic
import org.jetbrains.kotlin.resolve.calls.tower.isSynthesized

internal class PropertiesHighlightingVisitor(holder: AnnotationHolder, bindingContext: BindingContext)
    : AfterAnalysisHighlightingVisitor(holder, bindingContext) {

    override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
        if (expression.parent is KtThisExpression) {
            return
        }
        val target = bindingContext.get(BindingContext.REFERENCE_TARGET, expression)
        if (target is SyntheticFieldDescriptor) {
            highlightName(expression, BACKING_FIELD_VARIABLE)
            return
        }
        if (target !is PropertyDescriptor) {
            return
        }

        highlightProperty(expression, target)
    }

    override fun visitProperty(property: KtProperty) {
        val nameIdentifier = property.nameIdentifier ?: return
        val propertyDescriptor = bindingContext.get(BindingContext.VARIABLE, property)
        if (propertyDescriptor is PropertyDescriptor) {
            highlightProperty(nameIdentifier, propertyDescriptor)
        }

        super.visitProperty(property)
    }

    override fun visitParameter(parameter: KtParameter) {
        val nameIdentifier = parameter.nameIdentifier ?: return
        val propertyDescriptor = bindingContext.get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, parameter)
        if (propertyDescriptor != null) {
            if (propertyDescriptor.isVar) {
                highlightName(nameIdentifier, MUTABLE_VARIABLE)
            }
            highlightProperty(nameIdentifier, propertyDescriptor)
        }

        super.visitParameter(parameter)
    }

    private fun highlightProperty(
            elementToHighlight: PsiElement,
            descriptor: PropertyDescriptor) {

        if (applyHighlighterExtensions(elementToHighlight, descriptor)) return

        val attributesKey = when {
            descriptor.isDynamic() ->
                DYNAMIC_PROPERTY_CALL

            descriptor.extensionReceiverParameter != null ->
                if (descriptor.isSynthesized) SYNTHETIC_EXTENSION_PROPERTY else EXTENSION_PROPERTY

            DescriptorUtils.isStaticDeclaration(descriptor) ->
                PACKAGE_PROPERTY

            else ->
                INSTANCE_PROPERTY
        }
        highlightName(elementToHighlight, attributesKey)
    }
}
