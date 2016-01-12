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

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.move.moveInner.MoveInnerClassUsagesHandler
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.references.matchesTarget
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedElementSelector
import java.util.*

class MoveJavaInnerClassKotlinUsagesHandler: MoveInnerClassUsagesHandler {
    override fun correctInnerClassUsage(usage: UsageInfo, outerClass: PsiClass) {
        val innerCall = usage.element?.parent as? KtCallExpression ?: return

        val receiver = (innerCall.parent as? KtQualifiedExpression)?.receiverExpression ?: return
        val outerClassRefExpr = when (receiver) {
            is KtCallExpression -> receiver.calleeExpression
            is KtQualifiedExpression -> receiver.getQualifiedElementSelector()
            else -> null
        } as? KtSimpleNameExpression
        val outerClassRef = outerClassRefExpr?.mainReference ?: return
        if (!outerClassRef.matchesTarget(outerClass)) return

        val psiFactory = KtPsiFactory(usage.project)

        val argumentList = innerCall.valueArgumentList
        if (argumentList != null) {
            val newArguments = ArrayList<String>()
            newArguments.add(receiver.text!!)
            argumentList.arguments.mapTo(newArguments) { it.text!! }
            argumentList.replace(psiFactory.createCallArguments(newArguments.joinToString(prefix = "(", postfix = ")")))
        }
        else {
            innerCall.lambdaArguments.firstOrNull()?.let { lambdaArg ->
                val anchor = PsiTreeUtil.skipSiblingsBackward(lambdaArg, PsiWhiteSpace::class.java)
                innerCall.addAfter(psiFactory.createCallArguments("(${receiver.text})"), anchor)
            }
        }
    }
}