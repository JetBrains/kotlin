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

package org.jetbrains.kotlin.asJava.builder

import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolder
import com.intellij.psi.PsiMember
import com.intellij.psi.StubBasedPsiElement

data class MemberIndex(private val index: Int) {
    companion object {
        @JvmField
        val KEY = Key.create<MemberIndex>("MEMBER_INDEX")
    }
}

val PsiMember.memberIndex: MemberIndex?
    get() = ((this as? StubBasedPsiElement<*>)?.stub as? UserDataHolder)?.getUserData(MemberIndex.KEY)