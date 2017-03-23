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
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.RefactoringBundle
import com.intellij.usageView.UsageInfo
import com.intellij.usageView.UsageViewBundle
import com.intellij.usageView.UsageViewDescriptor
import org.jetbrains.kotlin.idea.codeInliner.CallableUsageReplacementStrategy
import org.jetbrains.kotlin.idea.codeInliner.replaceUsages
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.stubindex.KotlinSourceFilterScope
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtSimpleNameExpression

class KotlinInlineFunctionProcessor(
        project: Project,
        private val replacementStrategy: CallableUsageReplacementStrategy,
        private val function: KtNamedFunction,
        private val reference: KtSimpleNameReference?,
        private val inlineThisOnly: Boolean,
        private val deleteAfter: Boolean
) : BaseRefactoringProcessor(project) {

    private val commandName = RefactoringBundle.message("inline.method.command", DescriptiveNameUtil.getDescriptiveName(function))

    override fun findUsages(): Array<UsageInfo> {
        if (inlineThisOnly && reference != null) return arrayOf(UsageInfo(reference))
        val usages = runReadAction {
            val searchScope = KotlinSourceFilterScope.projectSources(GlobalSearchScope.projectScope(myProject), myProject)
            ReferencesSearch.search(function, searchScope).filterIsInstance<KtSimpleNameReference>()
        }
        return usages.map(::UsageInfo).toTypedArray()
    }

    override fun performRefactoring(usages: Array<out UsageInfo>) {
        replacementStrategy.replaceUsages(
                usages.mapNotNull { it.element as? KtSimpleNameExpression },
                function,
                myProject,
                commandName,
                { if (deleteAfter) function.delete() }
        )
    }

    override fun getCommandName(): String = commandName

    override fun createUsageViewDescriptor(usages: Array<out UsageInfo>): UsageViewDescriptor {
        return object : UsageViewDescriptor {
            override fun getCommentReferencesText(usagesCount: Int, filesCount: Int) =
                    RefactoringBundle.message("comments.elements.header", UsageViewBundle.getOccurencesString(usagesCount, filesCount))

            override fun getCodeReferencesText(usagesCount: Int, filesCount: Int) =
                    RefactoringBundle.message("invocations.to.be.inlined", UsageViewBundle.getReferencesString(usagesCount, filesCount))

            override fun getElements() = arrayOf(function)

            override fun getProcessedElementsHeader(): String =
                    RefactoringBundle.message("inline.method.elements.header")
        }
    }
}