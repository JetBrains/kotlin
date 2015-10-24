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

package org.jetbrains.kotlin.idea.liveTemplates.macro

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.template.Expression
import com.intellij.codeInsight.template.ExpressionContext
import com.intellij.codeInsight.template.Macro
import com.intellij.codeInsight.template.Result
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.idea.JetBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*

class JetAnonymousSuperMacro : Macro() {
    override fun getName(): String {
        return "anonymousSuper"
    }

    override fun getPresentableName(): String {
        return JetBundle.message("macro.fun.anonymousSuper")
    }

    override fun calculateResult(params: Array<Expression>, context: ExpressionContext): Result? {
        val editor = context.editor
        if (editor != null) {
            AnonymousTemplateEditingListener.registerListener(editor, context.project)
        }

        val vars = getSupertypes(params, context)
        if (vars == null || vars.size() == 0) return null
        return JetPsiElementResult(vars[0])
    }

    override fun calculateLookupItems(params: Array<Expression>, context: ExpressionContext): Array<LookupElement>? {
        val vars = getSupertypes(params, context)
        if (vars == null || vars.size < 2) return null
        val set = LinkedHashSet<LookupElement>()
        for (`var` in vars) {
            set.add(LookupElementBuilder.create(`var`))
        }
        return set.toArray<LookupElement>(arrayOfNulls<LookupElement>(set.size))
    }

    private fun getSupertypes(params: Array<Expression>, context: ExpressionContext): Array<PsiNamedElement>? {
        if (params.size() != 0) return null

        val project = context.project
        PsiDocumentManager.getInstance(project).commitAllDocuments()

        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(context.editor!!.document)
        if (psiFile !is KtFile) return null

        val expression = PsiTreeUtil.getParentOfType(psiFile.findElementAt(context.startOffset), KtExpression::class.java) ?: return null

        val bindingContext = expression.analyze(BodyResolveMode.FULL)
        val scope = bindingContext.get(BindingContext.RESOLUTION_SCOPE, expression) ?: return null

        val result = ArrayList<PsiNamedElement>()

        for (descriptor in scope.getDescriptors(DescriptorKindFilter.NON_SINGLETON_CLASSIFIERS)) {
            if (descriptor !is ClassDescriptor) continue
            if (!descriptor.modality.isOverridable) continue
            val kind = descriptor.kind
            if (kind == ClassKind.INTERFACE || kind == ClassKind.CLASS) {
                result.addIfNotNull(DescriptorToSourceUtils.descriptorToDeclaration(descriptor) as PsiNamedElement?)
            }
        }

        return result.toArray<PsiNamedElement>(arrayOfNulls<PsiNamedElement>(result.size))
    }
}
