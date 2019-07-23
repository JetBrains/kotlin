/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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

class MoveJavaInnerClassKotlinUsagesHandler : MoveInnerClassUsagesHandler {
    override fun correctInnerClassUsage(usage: UsageInfo, outerClass: PsiClass) {
        val innerCall = usage.element?.parent as? KtCallExpression ?: return
        val receiverExpression = innerCall.getQualifiedExpressionForSelector()?.receiverExpression

        val psiFactory = KtPsiFactory(usage.project)

        val argumentToAdd = psiFactory.createArgument(receiverExpression ?: psiFactory.createExpression("this"))

        val argumentList = innerCall.valueArgumentList
            ?: (innerCall.lambdaArguments.firstOrNull()?.let { lambdaArg ->
                val anchor = PsiTreeUtil.skipSiblingsBackward(lambdaArg, PsiWhiteSpace::class.java)
                innerCall.addAfter(psiFactory.createCallArguments("()"), anchor)
            } as KtValueArgumentList?)
            ?: return

        argumentList.addArgumentAfter(argumentToAdd, null)
    }
}