/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.codeInsight

import com.intellij.refactoring.RefactoringHelper
import com.intellij.usageView.UsageInfo
import com.intellij.openapi.project.Project
import java.util.HashSet
import com.intellij.openapi.util.Key
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.plugin.refactoring.changeQualifiedName
import org.jetbrains.jet.lang.psi.psiUtil.getQualifiedElementSelector
import org.jetbrains.jet.lang.psi.psiUtil.getOutermostNonInterleavingQualifiedElement
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.SmartPointerManager

public class KotlinShortenReferencesRefactoringHelper: RefactoringHelper<Set<SmartPsiElementPointer<JetSimpleNameExpression>>> {
    private val LOG = Logger.getInstance(javaClass<KotlinShortenReferencesRefactoringHelper>().getCanonicalName())!!

    override fun prepareOperation(usages: Array<out UsageInfo>?): Set<SmartPsiElementPointer<JetSimpleNameExpression>>? {
        if (usages != null && usages.isNotEmpty()) {
            val project = usages[0].getProject()
            val elementsToShorten = project.getElementsToShorten(false)
            if (elementsToShorten != null && !elementsToShorten.isEmpty()) {
                LOG.warn("Waiting set for reference shortening is not empty")
                project.clearElementsToShorten()
            }
        }
        return null
    }

    override fun performOperation(project: Project, operationData: Set<SmartPsiElementPointer<JetSimpleNameExpression>>?) {
        ApplicationManager.getApplication()!!.runWriteAction {
            project.getElementsToShorten(false)?.let { bindRequests ->
                project.clearElementsToShorten()
                ShortenReferences.process(
                        bindRequests
                                .map() { it.getElement()?.getOutermostNonInterleavingQualifiedElement() }
                                .filterNotNull()
                )
            }
        }
    }
}

private val ELEMENTS_TO_SHORTEN_KEY = Key.create<MutableSet<SmartPsiElementPointer<JetSimpleNameExpression>>>("ELEMENTS_TO_SHORTEN_KEY")

private fun Project.getElementsToShorten(createIfNeeded: Boolean): MutableSet<SmartPsiElementPointer<JetSimpleNameExpression>>? {
    var elementsToShorten = getUserData(ELEMENTS_TO_SHORTEN_KEY)
    if (createIfNeeded && elementsToShorten == null) {
        elementsToShorten = HashSet()
        putUserData(ELEMENTS_TO_SHORTEN_KEY, elementsToShorten)
    }

    return elementsToShorten
}

private fun Project.clearElementsToShorten() {
    putUserData(ELEMENTS_TO_SHORTEN_KEY, null)
}

public fun Project.addElementToShorteningWaitSet(expression: JetSimpleNameExpression) {
    assert (ApplicationManager.getApplication()!!.isWriteAccessAllowed(), "Write access needed")
    getElementsToShorten(true)!!.add(SmartPointerManager.getInstance(expression.getProject())!!.createSmartPsiElementPointer(expression))
}
