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
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.SmartPointerManager
import org.jetbrains.jet.lang.psi.JetElement
import org.jetbrains.jet.lang.psi.JetQualifiedExpression
import org.jetbrains.jet.lang.psi.JetUserType

public class KotlinShortenReferencesRefactoringHelper: RefactoringHelper<Any> {
    private val LOG = Logger.getInstance(javaClass<KotlinShortenReferencesRefactoringHelper>().getCanonicalName())!!

    override fun prepareOperation(usages: Array<out UsageInfo>?): Any? {
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

    override fun performOperation(project: Project, operationData: Any?) {
        ApplicationManager.getApplication()!!.runWriteAction {
            project.getElementsToShorten(false)?.let { bindRequests ->
                project.clearElementsToShorten()
                ShortenReferences.process(bindRequests.map() { it.getElement() }.filterNotNull())
            }
        }
    }
}

private val ELEMENTS_TO_SHORTEN_KEY = Key.create<MutableSet<SmartPsiElementPointer<JetElement>>>("ELEMENTS_TO_SHORTEN_KEY")

private fun Project.getElementsToShorten(createIfNeeded: Boolean): MutableSet<SmartPsiElementPointer<JetElement>>? {
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

public fun JetElement.addToShorteningWaitSet() {
    assert (this is JetQualifiedExpression || this is JetSimpleNameExpression || this is JetUserType, "Unexpected element type: ${getClass()}: ${getText()}")
    assert (ApplicationManager.getApplication()!!.isWriteAccessAllowed(), "Write access needed")
    val project = getProject()
    project.getElementsToShorten(true)!!.add(SmartPointerManager.getInstance(project)!!.createSmartPsiElementPointer(this))
}
