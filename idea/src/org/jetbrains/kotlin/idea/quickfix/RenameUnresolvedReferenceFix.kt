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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.daemon.QuickFixBundle
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.template.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.codeInsight.ReferenceVariantsHelper
import org.jetbrains.kotlin.idea.core.NotPropertiesService
import org.jetbrains.kotlin.idea.core.isVisible
import org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder.guessTypes
import org.jetbrains.kotlin.idea.util.psi.patternMatching.KotlinPsiUnifier
import org.jetbrains.kotlin.idea.util.psi.patternMatching.toRange
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.utils.ifEmpty
import java.util.*

object RenameUnresolvedReferenceActionFactory : KotlinSingleIntentionActionFactory() {
    override fun createAction(diagnostic: Diagnostic): IntentionAction? {
        val ref = diagnostic.psiElement as? KtNameReferenceExpression ?: return null
        return RenameUnresolvedReferenceFix(ref)
    }
}

class RenameUnresolvedReferenceFix(element: KtNameReferenceExpression): KotlinQuickFixAction<KtNameReferenceExpression>(element) {
    companion object {
        private val INPUT_VARIABLE_NAME = "INPUT_VAR"
        private val OTHER_VARIABLE_NAME = "OTHER_VAR"
    }

    private class ReferenceNameExpression(
            private val items: Array<out LookupElement>,
            private val originalReferenceName: String
    ) : Expression() {
        init {
            Arrays.sort(items, HammingComparator(originalReferenceName, { lookupString }))
        }

        override fun calculateResult(context: ExpressionContext) = TextResult(items.firstOrNull()?.lookupString ?: originalReferenceName)

        override fun calculateQuickResult(context: ExpressionContext) = null

        override fun calculateLookupItems(context: ExpressionContext) = if (items.size <= 1) null else items
    }

    override fun getText() = QuickFixBundle.message("rename.wrong.reference.text")

    override fun getFamilyName() = QuickFixBundle.message("rename.wrong.reference.family")

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean {
        val element = element ?: return false
        return super.isAvailable(project, editor, file)
               && editor != null
               && element.getStrictParentOfType<KtTypeReference>() == null
    }

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        if (editor == null) return
        val patternExpression = element.getQualifiedElement() as? KtExpression ?: return

        val originalName = element.getReferencedName()
        val container = element.parents.firstOrNull { it is KtDeclarationWithBody || it is KtClassOrObject || it is KtFile } ?: return
        val isCallee = element.isCallee()
        val occurrences = patternExpression.toRange()
                .match(container, KotlinPsiUnifier.DEFAULT)
                .mapNotNull {
                    val candidate = (it.range.elements.first() as? KtExpression)?.getQualifiedElementSelector() as? KtNameReferenceExpression
                    if (candidate != null && candidate.isCallee() == isCallee) candidate else null
                }

        val resolutionFacade = element.getResolutionFacade()
        val context = resolutionFacade.analyze(element, BodyResolveMode.PARTIAL)
        val moduleDescriptor = resolutionFacade.moduleDescriptor
        val variantsHelper = ReferenceVariantsHelper(context, resolutionFacade, moduleDescriptor, {
            it !is DeclarationDescriptorWithVisibility || it.isVisible(element, null, context, resolutionFacade)
        }, NotPropertiesService.getNotProperties(element))
        val expectedTypes = patternExpression
                .guessTypes(context, moduleDescriptor)
                .ifEmpty { arrayOf(moduleDescriptor.builtIns.nullableAnyType) }
        val descriptorKindFilter = if (isCallee) DescriptorKindFilter.FUNCTIONS else DescriptorKindFilter.VARIABLES
        val lookupItems = variantsHelper
                .getReferenceVariants(element, descriptorKindFilter, { true })
                .filter { candidate ->
                    candidate is CallableDescriptor && (expectedTypes.any { candidate.returnType?.isSubtypeOf(it) ?: false })
                }
                .mapTo(if (ApplicationManager.getApplication().isUnitTestMode) linkedSetOf() else linkedSetOf(originalName)) {
                    it.name.asString()
                }
                .map { LookupElementBuilder.create(it) }
                .toTypedArray()
        val nameExpression = ReferenceNameExpression(lookupItems, originalName)

        val builder = TemplateBuilderImpl(container)
        occurrences.forEach {
            if (it != element) {
                builder.replaceElement(it.getReferencedNameElement(), OTHER_VARIABLE_NAME, INPUT_VARIABLE_NAME, false)
            }
            else {
                builder.replaceElement(it.getReferencedNameElement(), INPUT_VARIABLE_NAME, nameExpression, true)
            }
        }

        editor.caretModel.moveToOffset(container.startOffset)
        TemplateManager.getInstance(project).startTemplate(editor, builder.buildInlineTemplate())
    }
}

private class HammingComparator<T>(private val referenceString: String, private val asString: T.() -> String) : Comparator<T> {
    private fun countDifference(s1: String): Int {
        return (0..Math.min(s1.lastIndex, referenceString.lastIndex)).count { s1[it] != referenceString[it] }
    }

    override fun compare(lookupItem1: T, lookupItem2: T): Int {
        return countDifference(lookupItem1.asString()) - countDifference(lookupItem2.asString())
    }
}
