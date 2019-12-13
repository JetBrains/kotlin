/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.liveTemplates.macro

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.template.Expression
import com.intellij.codeInsight.template.ExpressionContext
import com.intellij.codeInsight.template.Result
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.utils.collectDescriptorsFiltered

class AnonymousSuperMacro : KotlinMacro() {
    override fun getName() = "anonymousSuper"
    override fun getPresentableName() = "anonymousSuper()"

    override fun calculateResult(params: Array<Expression>, context: ExpressionContext): Result? {
        val editor = context.editor
        if (editor != null) {
            AnonymousTemplateEditingListener.registerListener(editor, context.project)
        }

        val vars = getSupertypes(params, context)
        if (vars.isEmpty()) return null
        return KotlinPsiElementResult(vars.first())
    }

    override fun calculateLookupItems(params: Array<Expression>, context: ExpressionContext): Array<LookupElement>? {
        val superTypes = getSupertypes(params, context)
        if (superTypes.size < 2) return null
        return superTypes.map { LookupElementBuilder.create(it) }.toTypedArray()
    }

    private fun getSupertypes(params: Array<Expression>, context: ExpressionContext): Collection<PsiNamedElement> {
        if (params.isNotEmpty()) return emptyList()

        val psiDocumentManager = PsiDocumentManager.getInstance(context.project)
        psiDocumentManager.commitAllDocuments()

        val psiFile = psiDocumentManager.getPsiFile(context.editor!!.document) as? KtFile ?: return emptyList()

        val expression = PsiTreeUtil.getParentOfType(psiFile.findElementAt(context.startOffset), KtExpression::class.java)
            ?: return emptyList()

        val bindingContext = expression.analyze(BodyResolveMode.FULL)
        val resolutionScope = expression.getResolutionScope(bindingContext, expression.getResolutionFacade())

        return resolutionScope.collectDescriptorsFiltered(DescriptorKindFilter.NON_SINGLETON_CLASSIFIERS)
            .filter {
                it is ClassDescriptor &&
                        (it.modality == Modality.OPEN || it.modality == Modality.ABSTRACT) &&
                        (it.kind == ClassKind.CLASS || it.kind == ClassKind.INTERFACE)
            }
            .mapNotNull { DescriptorToSourceUtils.descriptorToDeclaration(it) as PsiNamedElement? }
    }
}
