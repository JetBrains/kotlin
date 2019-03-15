/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.highlighter.KotlinHighlightingColors.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject

internal class TypeKindHighlightingVisitor(holder: AnnotationHolder, bindingContext: BindingContext) :
    AfterAnalysisHighlightingVisitor(holder, bindingContext) {

    override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
        val parent = expression.parent
        if (parent is KtSuperExpression || parent is KtThisExpression) {
            // Do nothing: 'super' and 'this' are highlighted as a keyword
            return
        }

        if (!NameHighlighter.namesHighlightingEnabled) return

        val referenceTarget = computeReferencedDescriptor(expression) ?: return

        val key = attributeKeyForObjectAccess(expression) ?: when (referenceTarget) {
            is TypeParameterDescriptor -> TYPE_PARAMETER
            is TypeAliasDescriptor -> TYPE_ALIAS
            !is ClassDescriptor -> return
            else -> when (referenceTarget.kind) {
                ClassKind.ANNOTATION_CLASS -> ANNOTATION
                else -> textAttributesKeyForClassDeclaration(referenceTarget)
            }
        }

        highlightName(computeHighlightingRangeForUsage(expression, referenceTarget), key)
    }

    private fun attributeKeyForObjectAccess(expression: KtSimpleNameExpression): TextAttributesKey? {
        val resolvedCall = expression.getResolvedCall(bindingContext)
        return if (resolvedCall?.resultingDescriptor is FakeCallableDescriptorForObject)
            attributeKeyForCallFromExtensions(expression, resolvedCall)
        else null
    }

    private fun computeReferencedDescriptor(expression: KtSimpleNameExpression): DeclarationDescriptor? {
        val referenceTarget = bindingContext.get(BindingContext.REFERENCE_TARGET, expression)

        if (referenceTarget !is ConstructorDescriptor) return referenceTarget

        val callElement = expression.getParentOfTypeAndBranch<KtCallExpression>(true) { calleeExpression }
            ?: expression.getParentOfTypeAndBranch<KtSuperTypeCallEntry>(true) { calleeExpression }

        if (callElement != null) {
            return referenceTarget
        }

        return referenceTarget.containingDeclaration
    }


    private fun computeHighlightingRangeForUsage(expression: KtSimpleNameExpression, referenceTarget: DeclarationDescriptor): TextRange {
        val expressionRange = expression.textRange

        if (referenceTarget !is ClassDescriptor || referenceTarget.kind != ClassKind.ANNOTATION_CLASS) return expressionRange

        // include '@' symbol if the reference is the first segment of KtAnnotationEntry
        // if "Deprecated" is highlighted then '@' should be highlighted too in "@Deprecated"
        val annotationEntry = PsiTreeUtil.getParentOfType(
            expression, KtAnnotationEntry::class.java, /* strict = */false, KtValueArgumentList::class.java
        )
        val atSymbol = annotationEntry?.atSymbol ?: return expressionRange
        return TextRange(atSymbol.textRange.startOffset, expression.textRange.endOffset)
    }

    override fun visitTypeParameter(parameter: KtTypeParameter) {
        parameter.nameIdentifier?.let { highlightName(it, TYPE_PARAMETER) }
        super.visitTypeParameter(parameter)
    }

    override fun visitClassOrObject(classOrObject: KtClassOrObject) {
        val identifier = classOrObject.nameIdentifier
        val classDescriptor = bindingContext.get(BindingContext.CLASS, classOrObject)
        if (identifier != null && classDescriptor != null) {
            highlightName(
                identifier,
                attributeKeyForDeclarationFromExtensions(classOrObject, classDescriptor)
                    ?: textAttributesKeyForClassDeclaration(classDescriptor)
            )
        }
        super.visitClassOrObject(classOrObject)
    }

    override fun visitTypeAlias(typeAlias: KtTypeAlias) {
        val identifier = typeAlias.nameIdentifier
        val descriptor = bindingContext.get(BindingContext.TYPE_ALIAS, typeAlias)
        if (identifier != null && descriptor != null) {
            highlightName(
                identifier,
                attributeKeyForDeclarationFromExtensions(identifier, descriptor) ?: TYPE_ALIAS
            )
        }
        super.visitTypeAlias(typeAlias)
    }

    override fun visitDynamicType(type: KtDynamicType) {
        // Do nothing: 'dynamic' is highlighted as a keyword
    }

    private fun textAttributesKeyForClassDeclaration(descriptor: ClassDescriptor): TextAttributesKey = when (descriptor.kind) {
        ClassKind.INTERFACE -> TRAIT
        ClassKind.ANNOTATION_CLASS -> ANNOTATION
        ClassKind.OBJECT -> OBJECT
        ClassKind.ENUM_ENTRY -> ENUM_ENTRY
        else -> if (descriptor.modality === Modality.ABSTRACT) ABSTRACT_CLASS else CLASS
    }
}
