/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.refactoring.rename

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.naming.AutomaticRenamer
import com.intellij.refactoring.rename.naming.AutomaticTestRenamerFactory
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.asJava.KtLightClass
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.psi.KtClassOrObject

class KotlinAutomaticTestRenamerFactory : AutomaticTestRenamerFactory() {
    private fun getPsiClass(element: PsiElement): PsiClass? {
        return when (element) {
            is KtLightClass -> element
            is KtClassOrObject -> element.toLightClass()
            else -> null
        }
    }

    override fun isApplicable(element: PsiElement): Boolean {
        val psiClass = getPsiClass(element) ?: return false
        return super.isApplicable(psiClass)
    }

    override fun createRenamer(element: PsiElement, newName: String?, usages: MutableCollection<UsageInfo>): AutomaticRenamer {
        return super.createRenamer(getPsiClass(element)!!, newName, usages)
    }
}