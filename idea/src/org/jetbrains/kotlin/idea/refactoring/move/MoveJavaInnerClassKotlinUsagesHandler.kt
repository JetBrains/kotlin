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

package org.jetbrains.kotlin.idea.refactoring.move

import com.intellij.refactoring.move.moveInner.MoveInnerClassUsagesHandler
import com.intellij.usageView.UsageInfo
import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.psi.JetQualifiedExpression
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedElementSelector
import org.jetbrains.kotlin.psi.JetCallExpression
import org.jetbrains.kotlin.psi.JetPsiFactory
import java.util.ArrayList
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.PsiWhiteSpace

public class MoveJavaInnerClassKotlinUsagesHandler: MoveInnerClassUsagesHandler {
    override fun correctInnerClassUsage(usage: UsageInfo, outerClass: PsiClass) {
        val innerCall = usage.getElement()?.getParent() as? JetCallExpression ?: return

        val receiver = (innerCall.getParent() as? JetQualifiedExpression)?.getReceiverExpression()
        val outerClassRef = when (receiver) {
            is JetCallExpression -> receiver.getCalleeExpression()
            is JetQualifiedExpression -> receiver.getQualifiedElementSelector()
            else -> null
        } as? JetSimpleNameExpression
        if (outerClassRef?.getReference()?.resolve() != outerClass) return

        val outerCall = outerClassRef!!.getParent() as? JetCallExpression ?: return

        val psiFactory = JetPsiFactory(usage.getProject())

        val argumentList = innerCall.getValueArgumentList()
        if (argumentList != null) {
            val newArguments = ArrayList<String>()
            newArguments.add(outerCall.getText()!!)
            argumentList.getArguments().mapTo(newArguments) { it.getText()!! }
            argumentList.replace(psiFactory.createCallArguments(newArguments.joinToString(prefix = "(", postfix = ")")))
        }
        else {
            innerCall.getFunctionLiteralArguments().firstOrNull()?.let { lambdaArg ->
                val anchor = PsiTreeUtil.skipSiblingsBackward(lambdaArg, javaClass<PsiWhiteSpace>())
                innerCall.addAfter(psiFactory.createCallArguments("(${outerCall.getText()})"), anchor)
            }
        }
    }
}
