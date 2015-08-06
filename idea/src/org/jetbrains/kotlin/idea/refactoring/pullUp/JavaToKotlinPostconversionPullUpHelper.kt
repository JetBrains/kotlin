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
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedElementSelector
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression
import java.util.LinkedHashSet

public class JavaToKotlinPostconversionPullUpHelper(private val data: PullUpData) : PullUpHelper<MemberInfo> {
    override fun setCorrectVisibility(info: MemberInfo?) { }

    override fun encodeContextInfo(info: MemberInfo?) { }

    override fun move(info: MemberInfo?, substitutor: PsiSubstitutor?) { }

    override fun postProcessMember(member: PsiMember?) { }

    // TODO: To be implemented
    override fun moveFieldInitializations(movedFields: LinkedHashSet<PsiField>?) { }

    override fun updateUsage(element: PsiElement?) {
        if (element !is JetSimpleNameExpression) return
        val qualifier = element.getReceiverExpression()?.getQualifiedElementSelector() as? JetSimpleNameExpression ?: return
        qualifier.mainReference.bindToElement(data.targetClass.unwrapped!!)
    }
}