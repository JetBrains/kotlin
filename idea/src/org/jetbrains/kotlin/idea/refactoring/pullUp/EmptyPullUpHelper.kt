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
import com.intellij.refactoring.classMembers.MemberInfoBase
import com.intellij.refactoring.memberPullUp.PullUpData
import com.intellij.refactoring.memberPullUp.PullUpHelper
import com.intellij.refactoring.memberPullUp.PullUpHelperFactory
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.psi.JetClassOrObject
import org.jetbrains.kotlin.psi.JetNamedDeclaration
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.substitutions.getTypeSubstitutor
import java.util.LinkedHashSet

object EmptyPullUpHelper : PullUpHelper<MemberInfoBase<PsiMember>> {
    override fun postProcessMember(member: PsiMember) {

    }

    override fun moveFieldInitializations(movedFields: LinkedHashSet<PsiField>) {

    }

    override fun encodeContextInfo(info: MemberInfoBase<PsiMember>) {

    }

    override fun setCorrectVisibility(info: MemberInfoBase<PsiMember>) {

    }

    override fun move(info: MemberInfoBase<PsiMember>, substitutor: PsiSubstitutor) {

    }

    override fun updateUsage(element: PsiElement) {

    }
}

