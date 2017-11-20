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

internal class TypeKindHighlightingVisitor(holder: AnnotationHolder, bindingContext: BindingContext)
        : AfterAnalysisHighlightingVisitor(holder, bindingContext) {

    override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
        val parent = expression.parent
        if (parent is KtSuperExpression || parent is KtThisExpression) {
            // Do nothing: 'super' and 'this' are highlighted as a keyword
            return
        }

        if (NameHighlighter.namesHighlightingEnabled) {
            var referenceTarget = bindingContext.get(BindingContext.REFERENCE_TARGET, expression)
            if (referenceTarget is ConstructorDescriptor) {
                val callElement = expression.getParentOfTypeAndBranch<KtCallExpression>(true) { calleeExpression }
                    ?: expression.getParentOfTypeAndBranch<KtSuperTypeCallEntry>(true) { calleeExpression }
                if (callElement == null) {
                    referenceTarget = referenceTarget.containingDeclaration
                }
            }

            if (referenceTarget is ClassDescriptor) {
                if (referenceTarget.kind == ClassKind.ANNOTATION_CLASS) {
                    highlightAnnotation(expression)
                }
                else {
                    highlightName(expression, textAttributesKeyForClass(referenceTarget))
                }
            }
            else if (referenceTarget is TypeParameterDescriptor) {
                highlightName(expression, TYPE_PARAMETER)
            }
        }
    }

    private fun highlightAnnotation(expression: KtSimpleNameExpression) {
        var range = expression.textRange

        // include '@' symbol if the reference is the first segment of KtAnnotationEntry
        // if "Deprecated" is highlighted then '@' should be highlighted too in "@Deprecated"
        val annotationEntry = PsiTreeUtil.getParentOfType(
                expression, KtAnnotationEntry::class.java, /* strict = */false, KtValueArgumentList::class.java)
        if (annotationEntry != null) {
            val atSymbol = annotationEntry.atSymbol
            if (atSymbol != null) {
                range = TextRange(atSymbol.textRange.startOffset, expression.textRange.endOffset)
            }
        }

        highlightName(range, ANNOTATION)
    }

    override fun visitTypeParameter(parameter: KtTypeParameter) {
        parameter.nameIdentifier?.let { highlightName(it, TYPE_PARAMETER) }
        super.visitTypeParameter(parameter)
    }

    override fun visitClassOrObject(classOrObject: KtClassOrObject) {
        val identifier = classOrObject.nameIdentifier
        val classDescriptor = bindingContext.get(BindingContext.CLASS, classOrObject)
        if (identifier != null && classDescriptor != null) {
            if (applyHighlighterExtensions(identifier, classDescriptor)) return
            highlightName(identifier, textAttributesKeyForClass(classDescriptor))
        }
        super.visitClassOrObject(classOrObject)
    }

    override fun visitDynamicType(type: KtDynamicType) {
        // Do nothing: 'dynamic' is highlighted as a keyword
    }

    private fun textAttributesKeyForClass(descriptor: ClassDescriptor): TextAttributesKey = when (descriptor.kind) {
        ClassKind.INTERFACE -> TRAIT
        ClassKind.ANNOTATION_CLASS -> ANNOTATION
        ClassKind.OBJECT -> OBJECT
        ClassKind.ENUM_ENTRY -> ENUM_ENTRY
        else -> if (descriptor.modality === Modality.ABSTRACT) ABSTRACT_CLASS else CLASS
    }
}
