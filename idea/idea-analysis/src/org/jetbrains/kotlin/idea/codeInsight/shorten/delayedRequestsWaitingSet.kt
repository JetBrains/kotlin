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

package org.jetbrains.kotlin.idea.codeInsight.shorten

import com.intellij.openapi.project.Project
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.openapi.util.Key
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.SmartPointerManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.idea.caches.resolve.util.getJavaMemberDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.core.ShortenReferences.Options
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import java.util.*

interface DelayedRefactoringRequest

class ShorteningRequest(val pointer: SmartPsiElementPointer<KtElement>, val options: Options) : DelayedRefactoringRequest
class ImportRequest(
        val elementToImportPointer: SmartPsiElementPointer<PsiElement>,
        val filePointer: SmartPsiElementPointer<KtFile>
) : DelayedRefactoringRequest

private var Project.delayedRefactoringRequests: MutableSet<DelayedRefactoringRequest>?
        by UserDataProperty(Key.create("DELAYED_REFACTORING_REQUESTS"))

/*
 * When one refactoring invokes another this value must be set to false so that shortening wait-set is not cleared
 * and previously collected references are processed correctly. Afterwards it must be reset to original value
 */
var Project.ensureNoRefactoringRequestsBeforeRefactoring: Boolean
        by NotNullableUserDataProperty(Key.create("ENSURE_NO_REFACTORING_REQUESTS_BEFORE_REFACTORING"), true)

fun Project.runRefactoringAndKeepDelayedRequests(action: () -> Unit) {
    val ensureNoRefactoringRequests = ensureNoRefactoringRequestsBeforeRefactoring

    try {
        ensureNoRefactoringRequestsBeforeRefactoring = false
        action()
    } finally {
        ensureNoRefactoringRequestsBeforeRefactoring = ensureNoRefactoringRequests
    }
}

private fun Project.getOrCreateRefactoringRequests(): MutableSet<DelayedRefactoringRequest> {
    var requests = delayedRefactoringRequests
    if (requests == null) {
        requests = LinkedHashSet()
        delayedRefactoringRequests = requests
    }

    return requests
}

fun KtElement.addToShorteningWaitSet(options: Options = Options.DEFAULT) {
    assert(ApplicationManager.getApplication()!!.isWriteAccessAllowed) { "Write access needed" }
    val project = project
    val elementPointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(this)
    project.getOrCreateRefactoringRequests().add(ShorteningRequest(elementPointer, options))
}

fun addDelayedImportRequest(elementToImport: PsiElement, file: KtFile) {
    assert(ApplicationManager.getApplication()!!.isWriteAccessAllowed) { "Write access needed" }
    file.project.getOrCreateRefactoringRequests() += ImportRequest(elementToImport.createSmartPointer(), file.createSmartPointer())
}

fun performDelayedRefactoringRequests(project: Project) {
    project.delayedRefactoringRequests?.let { requests ->
        project.delayedRefactoringRequests = null

        val shorteningRequests = ArrayList<ShorteningRequest>()
        val importRequests = ArrayList<ImportRequest>()
        requests.forEach {
            when (it) {
                is ShorteningRequest -> shorteningRequests += it
                is ImportRequest -> importRequests += it
            }
        }

        val elementToOptions = shorteningRequests.mapNotNull { req -> req.pointer.element?.let { it to req.options } }.toMap()
        val elements = elementToOptions.keys
        //TODO: this is not correct because it should not shorten deep into the elements!
        ShortenReferences({ elementToOptions[it] ?: Options.DEFAULT }).process(elements)

        val importInsertHelper = ImportInsertHelper.getInstance(project)

        for ((file, requestsForFile) in importRequests.groupBy { it.filePointer.element }) {
            if (file == null) continue

            for (requestForFile in requestsForFile) {
                val elementToImport = requestForFile.elementToImportPointer.element?.unwrapped ?: continue
                val descriptorToImport = when (elementToImport) {
                    is KtDeclaration -> elementToImport.unsafeResolveToDescriptor(BodyResolveMode.PARTIAL)
                    is PsiMember -> elementToImport.getJavaMemberDescriptor()
                    else -> null
                } ?: continue
                importInsertHelper.importDescriptor(file, descriptorToImport)
            }
        }
    }
}

private val LOG = Logger.getInstance(Project::class.java.canonicalName)

fun prepareDelayedRequests(project: Project) {
    val requests = project.delayedRefactoringRequests
    if (project.ensureNoRefactoringRequestsBeforeRefactoring && requests != null && !requests.isEmpty()) {
        LOG.warn("Waiting set for reference shortening is not empty")
        project.delayedRefactoringRequests = null
    }
}

var KtElement.isToBeShortened: Boolean? by CopyablePsiUserDataProperty(Key.create("IS_TO_BE_SHORTENED"))

fun KtElement.addToBeShortenedDescendantsToWaitingSet() {
    forEachDescendantOfType<KtElement> {
        if (it.isToBeShortened ?: false) {
            it.isToBeShortened = null
            it.addToShorteningWaitSet()
        }
    }
}