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
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector

class MoveJavaInnerClassKotlinUsagesHandler: MoveInnerClassUsagesHandler {
    override fun correctInnerClassUsage(usage: UsageInfo, outerClass: PsiClass) {
        val innerCall = usage.element?.parent as? KtCallExpression ?: return
        val receiverExpression = innerCall.getQualifiedExpressionForSelector()?.receiverExpression

        val psiFactory = KtPsiFactory(usage.project)

        val argumentToAdd = psiFactory.createArgument(receiverExpression ?: psiFactory.createExpression("this"))

        val argumentList =
                innerCall.valueArgumentList
                ?: (innerCall.lambdaArguments.firstOrNull()?.let { lambdaArg ->
                    val anchor = PsiTreeUtil.skipSiblingsBackward(lambdaArg, PsiWhiteSpace::class.java)
                    innerCall.addAfter(psiFactory.createCallArguments("()"), anchor)
                } as KtValueArgumentList?)
                ?: return

        argumentList.addArgumentAfter(argumentToAdd, null)
    }
}