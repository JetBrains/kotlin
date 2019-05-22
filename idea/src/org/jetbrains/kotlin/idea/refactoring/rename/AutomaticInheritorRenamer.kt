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

package org.jetbrains.kotlin.idea.refactoring.rename

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.search.searches.ClassInheritorsSearch
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringSettings
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.rename.naming.AutomaticRenamer
import com.intellij.refactoring.rename.naming.AutomaticRenamerFactory
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.psi.KtClass

class AutomaticInheritorRenamer(klass: KtClass, newName: String): AutomaticRenamer() {
    init {
        val lightClass = klass.toLightClass()
        if (lightClass != null) {
            for (inheritorLightClass in ClassInheritorsSearch.search(lightClass, true).findAll()) {
                if ((inheritorLightClass.unwrapped as? PsiNamedElement)?.name != null) {
                    myElements.add(inheritorLightClass.unwrapped as PsiNamedElement)
                }
            }
        }

        suggestAllNames(klass.name, newName)
    }

    override fun getDialogTitle() = RefactoringBundle.message("rename.inheritors.title")
    override fun getDialogDescription() = RefactoringBundle.message("rename.inheritors.with.the.following.names.to")
    override fun entityName() = RefactoringBundle.message("entity.name.inheritor")}

class AutomaticInheritorRenamerFactory : AutomaticRenamerFactory {
    override fun isApplicable(element: PsiElement) = element is KtClass
    override fun getOptionName() = RefactoringBundle.message("rename.inheritors")
    override fun isEnabled() = KotlinRefactoringSettings.instance.renameInheritors
    override fun setEnabled(enabled: Boolean) {
        KotlinRefactoringSettings.instance.renameInheritors = enabled
    }

    override fun createRenamer(element: PsiElement, newName: String, usages: Collection<UsageInfo>): AutomaticRenamer {
        return AutomaticInheritorRenamer(element as KtClass, newName)
    }
}
