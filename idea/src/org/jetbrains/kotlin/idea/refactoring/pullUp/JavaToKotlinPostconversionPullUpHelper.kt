/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.pullUp

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiSubstitutor
import com.intellij.refactoring.memberPullUp.PullUpData
import com.intellij.refactoring.memberPullUp.PullUpHelper
import com.intellij.refactoring.util.classMembers.MemberInfo
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedElementSelector
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression
import java.util.*

class JavaToKotlinPostconversionPullUpHelper(private val data: PullUpData) : PullUpHelper<MemberInfo> {
    override fun setCorrectVisibility(info: MemberInfo?) {}

    override fun encodeContextInfo(info: MemberInfo?) {}

    override fun move(info: MemberInfo?, substitutor: PsiSubstitutor?) {}

    override fun postProcessMember(member: PsiMember?) {}

    // TODO: To be implemented
    override fun moveFieldInitializations(movedFields: LinkedHashSet<PsiField>?) {}

    override fun updateUsage(element: PsiElement?) {
        if (element !is KtSimpleNameExpression) return
        val qualifier = element.getReceiverExpression()?.getQualifiedElementSelector() as? KtSimpleNameExpression ?: return
        qualifier.mainReference.bindToElement(data.targetClass.unwrapped!!)
    }
}