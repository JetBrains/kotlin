/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.android.quickfix

import com.android.SdkConstants
import com.intellij.codeInsight.FileModificationService
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.android.inspections.lint.AndroidLintQuickFix
import org.jetbrains.android.inspections.lint.AndroidQuickfixContexts
import org.jetbrains.android.util.AndroidBundle
import org.jetbrains.kotlin.android.hasBackingField
import org.jetbrains.kotlin.idea.util.addAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*


class SuppressLintQuickFix(id: String) : AndroidLintQuickFix {
    private val lintId = getLintId(id)

    override fun apply(startElement: PsiElement, endElement: PsiElement, context: AndroidQuickfixContexts.Context) {
        val annotationContainer = PsiTreeUtil.findFirstParent(startElement, true) { it.isSuppressLintTarget() } ?: return
        if (!FileModificationService.getInstance().preparePsiElementForWrite(annotationContainer)) {
            return
        }

        val argument = "\"$lintId\""

        when (annotationContainer) {
            is KtModifierListOwner -> {
                annotationContainer.addAnnotation(
                        FQNAME_SUPPRESS_LINT,
                        argument,
                        whiteSpaceText = if (annotationContainer.isNewLineNeededForAnnotation()) "\n" else " ",
                        addToExistingAnnotation = { entry -> addArgumentToAnnotation(entry, argument) })
            }
        }
    }

    override fun getName(): String = AndroidBundle.message(SUPPRESS_LINT_MESSAGE, lintId)

    override fun isApplicable(
            startElement: PsiElement,
            endElement: PsiElement,
            contextType: AndroidQuickfixContexts.ContextType
    ): Boolean = true

    private fun addArgumentToAnnotation(entry: KtAnnotationEntry, argument: String): Boolean {
        // add new arguments to an existing entry
        val args = entry.valueArgumentList
        val psiFactory = KtPsiFactory(entry)
        val newArgList = psiFactory.createCallArguments("($argument)")
        when {
            args == null -> // new argument list
                entry.addAfter(newArgList, entry.lastChild)
            args.arguments.isEmpty() -> // replace '()' with a new argument list
                args.replace(newArgList)
            args.arguments.none { it.textMatches(argument) } ->
                args.addArgument(newArgList.arguments[0])
        }

        return true
    }

    private fun getLintId(intentionId: String) =
            if (intentionId.startsWith(INTENTION_NAME_PREFIX)) intentionId.substring(INTENTION_NAME_PREFIX.length) else intentionId

    private fun KtElement.isNewLineNeededForAnnotation(): Boolean {
        return !(this is KtParameter ||
                 this is KtTypeParameter ||
                 this is KtPropertyAccessor)
    }

    private fun PsiElement.isSuppressLintTarget(): Boolean {
        return this is KtDeclaration &&
               (this as? KtProperty)?.hasBackingField() ?: true &&
               this !is KtFunctionLiteral &&
               this !is KtDestructuringDeclaration
    }
    private companion object {
        val INTENTION_NAME_PREFIX = "AndroidLint"
        val SUPPRESS_LINT_MESSAGE = "android.lint.fix.suppress.lint.api.annotation"
        val FQNAME_SUPPRESS_LINT = FqName(SdkConstants.FQCN_SUPPRESS_LINT)
    }
}