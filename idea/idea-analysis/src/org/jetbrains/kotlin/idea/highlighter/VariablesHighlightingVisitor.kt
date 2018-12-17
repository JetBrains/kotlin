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

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.SyntheticFieldDescriptor
import org.jetbrains.kotlin.idea.highlighter.KotlinHighlightingColors.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContext.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.MultipleSmartCasts
import org.jetbrains.kotlin.resolve.calls.tasks.isDynamic
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.kotlin.types.expressions.CaptureKind

internal class VariablesHighlightingVisitor(holder: AnnotationHolder, bindingContext: BindingContext)
    : AfterAnalysisHighlightingVisitor(holder, bindingContext) {

    override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
        val target = bindingContext.get(REFERENCE_TARGET, expression) ?: return
        if (target is ValueParameterDescriptor) {
            if (bindingContext.get(AUTO_CREATED_IT, target) == true) {
                createInfoAnnotation(expression, "Automatically declared based on the expected type")
                        .textAttributes = FUNCTION_LITERAL_DEFAULT_PARAMETER
            }
        }

        if (expression.parent !is KtValueArgumentName) { // highlighted separately
            highlightVariable(expression, target)
        }

        super.visitSimpleNameExpression(expression)
    }

    override fun visitProperty(property: KtProperty) {
        visitVariableDeclaration(property)
        super.visitProperty(property)
    }

    override fun visitParameter(parameter: KtParameter) {
        val propertyDescriptor = bindingContext.get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, parameter)
        if (propertyDescriptor == null) {
            visitVariableDeclaration(parameter)
        }
        super.visitParameter(parameter)
    }

    override fun visitDestructuringDeclarationEntry(multiDeclarationEntry: KtDestructuringDeclarationEntry) {
        visitVariableDeclaration(multiDeclarationEntry)
        super.visitDestructuringDeclarationEntry(multiDeclarationEntry)
    }

    private fun getSmartCastTarget(expression: KtExpression): PsiElement {
        var target: PsiElement = expression
        if (target is KtParenthesizedExpression) {
            target = KtPsiUtil.deparenthesize(target) ?: expression
        }
        return when (target) {
            is KtIfExpression -> target.ifKeyword
            is KtWhenExpression -> target.whenKeyword
            is KtBinaryExpression -> target.operationReference
            else -> target
        }
    }

    override fun visitExpression(expression: KtExpression) {
        val implicitSmartCast = bindingContext.get(IMPLICIT_RECEIVER_SMARTCAST, expression)
        if (implicitSmartCast != null) {
            for ((receiver, type) in implicitSmartCast.receiverTypes) {
                val receiverName = when (receiver) {
                    is ExtensionReceiver -> "Extension implicit receiver"
                    is ImplicitClassReceiver -> "Implicit receiver"
                    else -> "Unknown receiver"
                }
                createInfoAnnotation(expression,
                                     "$receiverName smart cast to " + DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(type))
                        .textAttributes = SMART_CAST_RECEIVER
            }
        }

        val nullSmartCast = bindingContext.get(SMARTCAST_NULL, expression) == true
        if (nullSmartCast) {
            createInfoAnnotation(expression, "Always null")
                    .textAttributes = SMART_CONSTANT
        }

        val smartCast = bindingContext.get(SMARTCAST, expression)
        if (smartCast != null) {
            val defaultType = smartCast.defaultType
            if (defaultType != null) {
                createInfoAnnotation(getSmartCastTarget(expression),
                                     "Smart cast to " + DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(defaultType))
                        .textAttributes = SMART_CAST_VALUE
            }
            else if (smartCast is MultipleSmartCasts) {
                for ((call, type) in smartCast.map) {
                    createInfoAnnotation(getSmartCastTarget(expression),
                                         "Smart cast to ${DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(type)} (for $call call)")
                            .textAttributes = SMART_CAST_VALUE
                }
            }
        }

        super.visitExpression(expression)
    }

    private fun visitVariableDeclaration(declaration: KtNamedDeclaration) {
        val declarationDescriptor = bindingContext.get(DECLARATION_TO_DESCRIPTOR, declaration)
        val nameIdentifier = declaration.nameIdentifier
        if (nameIdentifier != null && declarationDescriptor != null) {
            highlightVariable(nameIdentifier, declarationDescriptor)
        }
    }

    private fun highlightVariable(elementToHighlight: PsiElement, descriptor: DeclarationDescriptor) {
        if (descriptor is VariableDescriptor) {

            if (descriptor.isDynamic()) {
                highlightName(elementToHighlight, DYNAMIC_PROPERTY_CALL)
                return
            }

            if (descriptor.isVar) {
                highlightName(elementToHighlight, MUTABLE_VARIABLE)
            }

            if (bindingContext.get(CAPTURED_IN_CLOSURE, descriptor) == CaptureKind.NOT_INLINE) {
                val msg = if (descriptor.isVar)
                    "Wrapped into a reference object to be modified when captured in a closure"
                else
                    "Value captured in a closure"

                val parent = elementToHighlight.parent
                if (!(parent is PsiNameIdentifierOwner && parent.nameIdentifier == elementToHighlight)) {
                    createInfoAnnotation(elementToHighlight, msg).textAttributes = WRAPPED_INTO_REF
                    return
                }
            }

            if (descriptor is LocalVariableDescriptor && descriptor !is SyntheticFieldDescriptor) {
                highlightName(elementToHighlight, LOCAL_VARIABLE)
            }

            if (descriptor is ValueParameterDescriptor) {
                highlightName(elementToHighlight, PARAMETER)
            }
        }
    }
}
