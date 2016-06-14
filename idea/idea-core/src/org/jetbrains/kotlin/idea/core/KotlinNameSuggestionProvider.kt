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

package org.jetbrains.kotlin.idea.core

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiVariable
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.codeStyle.SuggestedNameInfo
import com.intellij.psi.statistics.JavaStatisticsManager
import com.intellij.refactoring.rename.NameSuggestionProvider
import org.jetbrains.kotlin.asJava.toLightElements
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

class KotlinNameSuggestionProvider : NameSuggestionProvider {
    override fun getSuggestedNames(element: PsiElement, nameSuggestionContext: PsiElement?, result: MutableSet<String>): SuggestedNameInfo? {
        if (element is KtCallableDeclaration) {
            val context = nameSuggestionContext ?: element.parent
            val target = if (element is KtProperty || element is KtParameter) {
                NewDeclarationNameValidator.Target.VARIABLES
            }
            else {
                NewDeclarationNameValidator.Target.FUNCTIONS_AND_CLASSES
            }
            val validator = NewDeclarationNameValidator(context, element, target, listOf(element))
            val names = SmartList<String>().apply {
                val name = element.name
                if (!name.isNullOrBlank()) {
                    this += KotlinNameSuggester.getCamelNames(name!!, validator, name.first().isLowerCase())
                }

                val type = (element.resolveToDescriptor() as CallableDescriptor).returnType
                if (type != null) {
                    this += KotlinNameSuggester.suggestNamesByType(type, validator)
                }
            }
            result += names

            return object : SuggestedNameInfo(names.toTypedArray()) {
                override fun nameChosen(name: String?) {
                    val psiVariable = element.toLightElements().firstIsInstanceOrNull<PsiVariable>() ?: return
                    JavaStatisticsManager.incVariableNameUseCount(
                            name,
                            JavaCodeStyleManager.getInstance(element.project).getVariableKind(psiVariable),
                            psiVariable.name,
                            psiVariable.type
                    )
                }
            }
        }

        return null
    }
}
