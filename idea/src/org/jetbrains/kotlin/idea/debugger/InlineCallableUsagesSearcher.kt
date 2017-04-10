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

package org.jetbrains.kotlin.idea.debugger

import com.intellij.debugger.engine.DebugProcess
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.ui.MessageType
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.xdebugger.impl.XDebugSessionImpl
import org.jetbrains.kotlin.idea.search.usagesSearch.isImportUsage
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.idea.debugger.DebuggerClassNameProvider.Companion.getRelevantElement
import org.jetbrains.kotlin.idea.debugger.evaluate.KotlinDebuggerCaches.ComputedClassNames

class InlineCallableUsagesSearcher(val myDebugProcess: DebugProcess, val scopes: List<GlobalSearchScope>) {
    fun findInlinedCalls(function: KtNamedFunction, context: BindingContext, transformer: (PsiElement) -> ComputedClassNames): ComputedClassNames {
        if (!InlineUtil.isInline(context.get(BindingContext.DECLARATION_TO_DESCRIPTOR, function))) {
            return ComputedClassNames.EMPTY
        }
        else {
            val searchResult = hashSetOf<PsiElement>()
            val functionName = runReadAction { function.name }

            val task = Runnable {
                ReferencesSearch.search(function, getScopeForInlineFunctionUsages(function)).forEach {
                    if (!runReadAction { it.isImportUsage() }) {
                        val usage = (it.element as? KtElement)?.let(::getRelevantElement)
                        if (usage != null) {
                            searchResult.add(usage)
                        }
                    }
                }
            }

            var isSuccess = true
            val applicationEx = ApplicationManagerEx.getApplicationEx()
            if (!applicationEx.isUnitTestMode && (!applicationEx.holdsReadLock() || applicationEx.isDispatchThread)) {
                applicationEx.invokeAndWait(
                        {
                            isSuccess = ProgressManager.getInstance().runProcessWithProgressSynchronously(
                                    task,
                                    "Compute class names for function $functionName",
                                    true,
                                    myDebugProcess.project)
                        }, ModalityState.NON_MODAL)
            }
            else {
                // Pooled thread with read lock. Can't invoke task under UI progress, so call it directly.
                task.run()
            }

            if (!isSuccess) {
                XDebugSessionImpl.NOTIFICATION_GROUP.createNotification(
                        "Debugger can skip some executions of $functionName method, because the computation of class names was interrupted", MessageType.WARNING
                ).notify(myDebugProcess.project)
            }

            val results = searchResult.map { transformer(it) }
            return ComputedClassNames(results.flatMap { it.classNames }, shouldBeCached = results.all { it.shouldBeCached })
        }
    }

    private fun getScopeForInlineFunctionUsages(inlineFunction: KtNamedFunction): GlobalSearchScope {
        val virtualFile = runReadAction { inlineFunction.containingFile.virtualFile }
        if (virtualFile != null && ProjectRootsUtil.isLibraryFile(myDebugProcess.project, virtualFile)) {
            return GlobalSearchScope.union(scopes.toTypedArray())
        }
        else {
            return myDebugProcess.searchScope
        }
    }
}