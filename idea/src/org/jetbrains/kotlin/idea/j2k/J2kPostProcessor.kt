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

package org.jetbrains.kotlin.idea.j2k

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.codeStyle.CodeStyleManager
import kotlinx.coroutines.experimental.run
import kotlinx.coroutines.experimental.runBlocking
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.conversion.copy.range
import org.jetbrains.kotlin.idea.core.util.EDT
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.j2k.PostProcessor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.elementsInRange
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import java.util.*

class J2kPostProcessor(private val formatCode: Boolean) : PostProcessor {
    override fun insertImport(file: KtFile, fqName: FqName) {
        ApplicationManager.getApplication().invokeAndWait({
            runWriteAction {
                val descriptors = file.resolveImportReference(fqName)
                descriptors.firstOrNull()?.let { ImportInsertHelper.getInstance(file.project).importDescriptor(file, it) }
            }
        }, ModalityState.defaultModalityState())
    }

    private enum class RangeFilterResult {
        SKIP,
        GO_INSIDE,
        PROCESS
    }

    override fun doAdditionalProcessing(file: KtFile, rangeMarker: RangeMarker?) =
            runBlocking(EDT.ModalityStateElement(ModalityState.any())) {
                do {
                    var modificationStamp: Long? = file.modificationStamp
                    val elementToActions = runReadAction {
                        collectAvailableActions(file, rangeMarker)
                    }

                    run(EDT) {
                        for ((element, action, _) in elementToActions) {
                            if (element.isValid) {
                                action()
                            }
                            else {
                                modificationStamp = null
                            }
                        }
                    }

                    if (modificationStamp == file.modificationStamp) break
                }
                while (elementToActions.isNotEmpty())


                if (formatCode) {
                    run(EDT) {
                        runWriteAction {
                            val codeStyleManager = CodeStyleManager.getInstance(file.project)
                            if (rangeMarker != null) {
                                if (rangeMarker.isValid) {
                                    codeStyleManager.reformatRange(file, rangeMarker.startOffset, rangeMarker.endOffset)
                                }
                            }
                            else {
                                codeStyleManager.reformat(file)
                            }
                            Unit
                        }
                    }
                }
            }


    private data class ActionData(val element: KtElement, val action: () -> Unit, val priority: Int)

    private fun collectAvailableActions(file: KtFile, rangeMarker: RangeMarker?): List<ActionData> {
        val diagnostics = analyzeFileRange(file, rangeMarker)

        val availableActions = ArrayList<ActionData>()

        file.accept(object : PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element is KtElement) {
                    val rangeResult = rangeFilter(element, rangeMarker)
                    if (rangeResult == RangeFilterResult.SKIP) return

                    super.visitElement(element)

                    if (rangeResult == RangeFilterResult.PROCESS) {
                        J2KPostProcessingRegistrar.processings.forEach { processing ->
                            val action = processing.createAction(element, diagnostics)
                            if (action != null) {
                                availableActions.add(ActionData(element, action, J2KPostProcessingRegistrar.priority(processing)))
                            }
                        }
                    }
                }
            }
        })
        availableActions.sortBy { it.priority }
        return availableActions
    }

    private fun analyzeFileRange(file: KtFile, rangeMarker: RangeMarker?): Diagnostics {
        val elements = if (rangeMarker == null)
            listOf(file)
        else
            file.elementsInRange(rangeMarker.range!!).filterIsInstance<KtElement>()

        return if (elements.isNotEmpty())
            file.getResolutionFacade().analyzeFullyAndGetResult(elements).bindingContext.diagnostics
        else
            Diagnostics.EMPTY
    }

    private fun rangeFilter(element: PsiElement, rangeMarker: RangeMarker?): RangeFilterResult {
        if (rangeMarker == null) return RangeFilterResult.PROCESS
        if (!rangeMarker.isValid) return RangeFilterResult.SKIP
        val range = TextRange(rangeMarker.startOffset, rangeMarker.endOffset)
        val elementRange = element.textRange
        return when {
            range.contains(elementRange) -> RangeFilterResult.PROCESS
            range.intersects(elementRange) -> RangeFilterResult.GO_INSIDE
            else -> RangeFilterResult.SKIP
        }
    }
}
