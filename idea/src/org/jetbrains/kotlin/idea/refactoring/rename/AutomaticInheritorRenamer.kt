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

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.refactoring.JavaRefactoringSettings
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.rename.naming.AutomaticRenamer
import com.intellij.refactoring.rename.naming.AutomaticRenamerFactory
import com.intellij.refactoring.rename.naming.InheritorRenamer
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.psi.JetClass

public class AutomaticInheritorRenamer(klass: JetClass, newName: String): AutomaticRenamer() {
    init {
        val lightClass = LightClassUtil.getPsiClass(klass)
        if (lightClass != null) {
            for (inheritorLightClass in ClassInheritorsSearch.search(lightClass, true).findAll()) {
                if ((inheritorLightClass.unwrapped as? PsiNamedElement)?.getName() != null) {
                    myElements.add(inheritorLightClass.unwrapped as PsiNamedElement)
                }
            }
        }

        suggestAllNames(klass.getName(), newName)
    }

    override fun getDialogTitle() = RefactoringBundle.message("rename.inheritors.title")
    override fun getDialogDescription() = RefactoringBundle.message("rename.inheritors.with.the.following.names.to")
    override fun entityName() = RefactoringBundle.message("entity.name.inheritor")
}

public class AutomaticInheritorRenamerFactory : AutomaticRenamerFactory {
    override fun isApplicable(element: PsiElement) = element is JetClass
    override fun getOptionName() = RefactoringBundle.message("rename.inheritors")
    override fun isEnabled() = JavaRefactoringSettings.getInstance().isToRenameInheritors()
    override fun setEnabled(enabled: Boolean) = JavaRefactoringSettings.getInstance().setRenameInheritors(enabled)

    override fun createRenamer(element: PsiElement, newName: String, usages: Collection<UsageInfo>): AutomaticRenamer {
        return AutomaticInheritorRenamer(element as JetClass, newName)
    }
}
