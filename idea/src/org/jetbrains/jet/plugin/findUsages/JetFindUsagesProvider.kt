/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.findUsages

import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import org.jetbrains.jet.lang.psi.*

public class JetFindUsagesProvider : FindUsagesProvider {
    public override fun canFindUsagesFor(psiElement: PsiElement): Boolean =
            psiElement is JetNamedDeclaration

    public override fun getWordsScanner(): WordsScanner =
            KotlinWordsScanner()

    public override fun getHelpId(psiElement: PsiElement): String? = null

    public override fun getType(element: PsiElement): String {
        return when(element) {
            is JetNamedFunction -> "function"
            is JetClass -> "class"
            is JetParameter -> "parameter"
            is JetProperty -> "property"
            else -> ""
        }
    }

    public override fun getDescriptiveName(element: PsiElement): String {
        return if (element is PsiNamedElement) element.getName() ?: "<unnamed>" else ""
    }

    public override fun getNodeText(element: PsiElement, useFullName: Boolean): String =
            getDescriptiveName(element)
}
