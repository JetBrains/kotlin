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

package org.jetbrains.kotlin.idea.search.ideaExtensions

import com.intellij.codeInsight.JavaTargetElementEvaluator
import com.intellij.codeInsight.TargetElementEvaluatorEx
import com.intellij.codeInsight.TargetElementUtil
import com.intellij.codeInsight.TargetElementUtilExtender
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import com.intellij.util.BitUtil
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithSource
import org.jetbrains.kotlin.idea.intentions.isAutoCreatedItUsage
import org.jetbrains.kotlin.idea.references.KtDestructuringDeclarationReference
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.references.resolveMainReferenceToDescriptors
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.source.getPsi

class KotlinTargetElementEvaluator : TargetElementEvaluatorEx, TargetElementUtilExtender {
    companion object {
        const val DO_NOT_UNWRAP_LABELED_EXPRESSION = 0x100
        const val BYPASS_IMPORT_ALIAS = 0x200

        // Place caret after the open curly brace in lambda for generated 'it'
        fun findLambdaOpenLBraceForGeneratedIt(ref: PsiReference): PsiElement? {
            val element: PsiElement = ref.element
            if (element.text != "it") return null

            if (element !is KtNameReferenceExpression || !isAutoCreatedItUsage(element)) return null

            val itDescriptor = element.resolveMainReferenceToDescriptors().singleOrNull() ?: return null
            val descriptorWithSource = itDescriptor.containingDeclaration as? DeclarationDescriptorWithSource ?: return null
            val lambdaExpression = descriptorWithSource.source.getPsi()?.parent as? KtLambdaExpression ?: return null
            return lambdaExpression.leftCurlyBrace.treeNext?.psi
        }

        // Navigate to receiver element for this in extension declaration
        fun findReceiverForThisInExtensionFunction(ref: PsiReference): PsiElement? {
            val element: PsiElement = ref.element
            if (element.text != "this") return null

            if (element !is KtNameReferenceExpression) return null
            val callableDescriptor = element.resolveMainReferenceToDescriptors().singleOrNull() as? CallableDescriptor ?: return null

            if (!callableDescriptor.isExtension) return null
            val callableDeclaration = callableDescriptor.source.getPsi() as? KtCallableDeclaration ?: return null

            return callableDeclaration.receiverTypeReference
        }
    }

    override fun getAdditionalDefinitionSearchFlags() = 0

    override fun getAdditionalReferenceSearchFlags() = DO_NOT_UNWRAP_LABELED_EXPRESSION or BYPASS_IMPORT_ALIAS

    override fun getAllAdditionalFlags() = additionalDefinitionSearchFlags + additionalReferenceSearchFlags

    override fun includeSelfInGotoImplementation(element: PsiElement): Boolean = !(element is KtClass && element.isAbstract())

    override fun getElementByReference(ref: PsiReference, flags: Int): PsiElement? {
        if (ref is KtSimpleNameReference && ref.expression is KtLabelReferenceExpression) {
            val refTarget = ref.resolve() as? KtExpression ?: return null
            if (!BitUtil.isSet(flags, DO_NOT_UNWRAP_LABELED_EXPRESSION)) {
                return refTarget.getLabeledParent(ref.expression.getReferencedName()) ?: refTarget
            }
            return refTarget
        }

        if (!BitUtil.isSet(flags, BYPASS_IMPORT_ALIAS)) {
            (ref.element as? KtSimpleNameExpression)?.mainReference?.getImportAlias()?.let { return it }
        }

        // prefer destructing declaration entry to its target if element name is accepted
        if (ref is KtDestructuringDeclarationReference && BitUtil.isSet(flags, TargetElementUtil.ELEMENT_NAME_ACCEPTED)) {
            return ref.element
        }

        val refExpression = ref.element as? KtSimpleNameExpression
        val calleeExpression = refExpression?.getParentOfTypeAndBranch<KtCallElement> { calleeExpression }
        if (calleeExpression != null) {
            (ref.resolve() as? KtConstructor<*>)?.let {
                return if (flags and JavaTargetElementEvaluator().additionalReferenceSearchFlags != 0) it else it.containingClassOrObject
            }
        }

        if (BitUtil.isSet(flags, TargetElementUtil.REFERENCED_ELEMENT_ACCEPTED)) {
            return findLambdaOpenLBraceForGeneratedIt(ref)
                    ?: findReceiverForThisInExtensionFunction(ref)
        }

        return null
    }

    override fun isIdentifierPart(file: PsiFile, text: CharSequence?, offset: Int): Boolean {
        val elementAtCaret = file.findElementAt(offset)

        if (elementAtCaret?.node?.elementType == KtTokens.IDENTIFIER) return true
        // '(' is considered identifier part if it belongs to primary constructor without 'constructor' keyword
        return elementAtCaret?.getNonStrictParentOfType<KtPrimaryConstructor>()?.textOffset == offset
    }
}
