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

package org.jetbrains.jet.plugin.intentions

import org.jetbrains.jet.lang.psi.JetForExpression
import com.intellij.openapi.editor.Editor
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.lang.psi.JetDotQualifiedExpression
import org.jetbrains.jet.lang.psi.JetPsiUtil
import org.jetbrains.jet.lang.cfg.PseudocodeVariablesData.VariableUseState
import org.jetbrains.jet.lang.cfg.pseudocode.PseudocodeUtil
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.resolve.ObservableBindingTrace
import org.jetbrains.jet.lang.resolve.BindingTraceContext
import org.jetbrains.jet.lang.cfg.JetFlowInformationProvider
import org.jetbrains.jet.lang.cfg.JetControlFlowProcessor
import com.intellij.debugger.jdi.LocalVariablesUtil
import com.siyeh.ig.psiutils.VariableAccessUtils
import com.google.dart.compiler.util.AstUtil
import com.intellij.find.FindManager
import com.intellij.find.impl.FindManagerImpl
import com.intellij.usageView.UsageInfo
import com.intellij.util.Processor
import org.jetbrains.jet.plugin.findUsages.KotlinPropertyFindUsagesOptions

public class RemoveForLoopIndicesIntention : JetSelfTargetingIntention<JetForExpression>(
        "remove.for.loop.indices", javaClass()) {
    override fun applyTo(element: JetForExpression, editor: Editor) {
        val parameter = element.getMultiParameter()!!
        val range = element.getLoopRange() as JetDotQualifiedExpression
        val parameters = parameter.getEntries()
        if (parameters.size() == 2) {
            parameter.replace(parameters[1])
        } else {
            JetPsiUtil.deleteElementWithDelimiters(parameters[0])
        }

        range.replace(range.getReceiverExpression())
    }

    override fun isApplicableTo(element: JetForExpression): Boolean {
        if (element.getMultiParameter() == null) return false
        val range = element.getLoopRange() as? JetDotQualifiedExpression ?: return false
        val selector = range.getSelectorExpression() ?: return false
        if (!selector.textMatches("withIndices()")) return false

        val indexVar = element.getMultiParameter()!!.getEntries()[0]

        val findManager = FindManager.getInstance(element.getProject()) as FindManagerImpl
        val findHandler = findManager.getFindUsagesManager().getFindUsagesHandler(indexVar,false) ?: return false
        val options = KotlinPropertyFindUsagesOptions(element.getProject())
        val usageCount = array(0)
        val usageFinderRunnable = object : Runnable {
            override fun run() {
                val processor = object : Processor<UsageInfo> {
                    override fun process(t: UsageInfo?): Boolean {
                        usageCount[0] = usageCount[0] + 1
                        return true
                    }
                }
                findHandler.processElementUsages(indexVar,processor,options)
            }
        }
        usageFinderRunnable.run()
        return usageCount[0] == 0
    }

}