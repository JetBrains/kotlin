/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.intentions

import org.jetbrains.jet.lang.psi.JetForExpression
import com.intellij.openapi.editor.Editor
import org.jetbrains.jet.lang.psi.JetDotQualifiedExpression
import org.jetbrains.jet.lang.psi.JetPsiUtil
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache
import com.intellij.find.FindManager
import com.intellij.find.impl.FindManagerImpl
import com.intellij.usageView.UsageInfo
import org.jetbrains.jet.plugin.findUsages.KotlinPropertyFindUsagesOptions
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.psi.JetCallExpression
import org.jetbrains.jet.lang.resolve.DescriptorUtils

public class RemoveForLoopIndicesIntention : JetSelfTargetingIntention<JetForExpression>(
        "remove.for.loop.indices", javaClass()) {
    override fun applyTo(element: JetForExpression, editor: Editor) {
        val parameter = element.getMultiParameter()!!
        val range = element.getLoopRange() as JetDotQualifiedExpression
        val parameters = parameter.getEntries()
        if (parameters.size() == 2) {
            parameter.replace(parameters[1])
        }
        else {
            JetPsiUtil.deleteElementWithDelimiters(parameters[0])
        }

        range.replace(range.getReceiverExpression())
    }

    override fun isApplicableTo(element: JetForExpression): Boolean {
        val multiParameter = element.getMultiParameter() ?: return false
        val range = element.getLoopRange() as? JetDotQualifiedExpression ?: return false
        val selector = range.getSelectorExpression() as? JetCallExpression ?: return false

        if (!selector.textMatches("withIndices()")) return false

        val bindingContext = AnalyzerFacadeWithCache.getContextForElement(element)
        val callResolution = bindingContext[BindingContext.RESOLVED_CALL, selector.getCalleeExpression()!!] ?: return false
        val fqName = DescriptorUtils.getFqNameSafe(callResolution.getCandidateDescriptor())
        if (fqName.toString() != "kotlin.withIndices") return false

        val indexVar = multiParameter.getEntries()[0]
        val findManager = FindManager.getInstance(element.getProject()) as FindManagerImpl
        val findHandler = findManager.getFindUsagesManager().getFindUsagesHandler(indexVar, false) ?: return false
        val options = KotlinPropertyFindUsagesOptions(element.getProject())
        var usageCount = 0
        val processorLambda: (UsageInfo?) -> Boolean = { t -> usageCount++; false }
        findHandler.processElementUsages(indexVar, processorLambda, options)
        return usageCount == 0
    }

}