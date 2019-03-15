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

package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.lang.findUsages.DescriptiveNameUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.usageView.UsageInfo
import com.intellij.usageView.UsageViewBundle
import com.intellij.usageView.UsageViewDescriptor
import org.jetbrains.kotlin.idea.codeInliner.UsageReplacementStrategy
import org.jetbrains.kotlin.idea.codeInliner.replaceUsages
import org.jetbrains.kotlin.idea.findUsages.ReferencesSearchScopeHelper
import org.jetbrains.kotlin.idea.refactoring.pullUp.deleteWithCompanion
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.*

class KotlinInlineCallableProcessor(
    project: Project,
    private val replacementStrategy: UsageReplacementStrategy,
    private val declaration: KtCallableDeclaration,
    private val reference: KtSimpleNameReference?,
    private val inlineThisOnly: Boolean,
    private val deleteAfter: Boolean,
    private val statementToDelete: KtBinaryExpression? = null
) : BaseRefactoringProcessor(project) {

    private val kind = when (declaration) {
        is KtNamedFunction -> "function"
        is KtProperty -> if (declaration.isLocal) "local variable" else "property"
        else -> "declaration"
    }

    private val commandName = "Inlining $kind ${DescriptiveNameUtil.getDescriptiveName(declaration)}"

    override fun findUsages(): Array<UsageInfo> {
        if (inlineThisOnly && reference != null) return arrayOf(UsageInfo(reference))
        val usages = runReadAction {
            val searchScope = GlobalSearchScope.projectScope(myProject)
            ReferencesSearchScopeHelper.search(declaration, searchScope)
        }
        return usages.map(::UsageInfo).toTypedArray()
    }

    override fun performRefactoring(usages: Array<out UsageInfo>) {
        val simpleNameUsages = usages.mapNotNull { it.element as? KtSimpleNameExpression }
        replacementStrategy.replaceUsages(
            simpleNameUsages,
            declaration,
            myProject,
            commandName,
            postAction = {
                if (deleteAfter) {
                    if (usages.size == simpleNameUsages.size) {
                        declaration.deleteWithCompanion()
                        statementToDelete?.delete()
                    } else {
                        CommonRefactoringUtil.showErrorHint(
                            declaration.project,
                            null,
                            "Cannot inline ${usages.size - simpleNameUsages.size}/${usages.size} usages",
                            "Inline $kind",
                            null
                        )
                    }
                }
            }
        )
    }

    override fun getCommandName(): String = commandName

    override fun createUsageViewDescriptor(usages: Array<out UsageInfo>): UsageViewDescriptor {
        return object : UsageViewDescriptor {
            override fun getCommentReferencesText(usagesCount: Int, filesCount: Int) =
                RefactoringBundle.message("comments.elements.header", UsageViewBundle.getOccurencesString(usagesCount, filesCount))

            override fun getCodeReferencesText(usagesCount: Int, filesCount: Int) =
                RefactoringBundle.message("invocations.to.be.inlined", UsageViewBundle.getReferencesString(usagesCount, filesCount))

            override fun getElements() = arrayOf(declaration)

            override fun getProcessedElementsHeader() = "${kind.capitalize()} to inline"
        }
    }
}