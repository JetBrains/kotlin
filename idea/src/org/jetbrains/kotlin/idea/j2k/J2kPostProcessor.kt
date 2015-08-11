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

import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.SmartList
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.quickfix.RemoveModifierFix
import org.jetbrains.kotlin.idea.quickfix.RemoveRightPartOfBinaryExpressionFix
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.j2k.PostProcessor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.elementsInRange
import org.jetbrains.kotlin.resolve.BindingContext
import java.util.HashSet
import java.util.LinkedHashMap

public class J2kPostProcessor(private val formatCode: Boolean) : PostProcessor {
    override fun analyzeFile(file: JetFile, range: TextRange?): BindingContext {
        val elements = if (range == null) {
            listOf(file)
        }
        else {
            file.elementsInRange(range).filterIsInstance<JetElement>()
        }
        if (elements.isEmpty()) return BindingContext.EMPTY
        return file.getResolutionFacade().analyzeFullyAndGetResult(elements).bindingContext
    }

    override fun insertImport(file: JetFile, fqName: FqName) {
        val descriptors = file.resolveImportReference(fqName)
        descriptors.firstOrNull()?.let { ImportInsertHelper.getInstance(file.getProject()).importDescriptor(file, it) }
    }

    override fun fixForProblem(problem: Diagnostic): (() -> Unit)? {
        val psiElement = problem.getPsiElement()
        return when (problem.getFactory()) {
            Errors.USELESS_CAST -> { ->
                val expression = RemoveRightPartOfBinaryExpressionFix(psiElement as JetBinaryExpressionWithTypeRHS, "").invoke()

                val variable = expression.getParent() as? JetProperty
                if (variable != null && expression == variable.getInitializer() && variable.isLocal()) {
                    val refs = ReferencesSearch.search(variable, LocalSearchScope(variable.getContainingFile())).findAll()
                    for (ref in refs) {
                        val usage = ref.getElement() as? JetSimpleNameExpression ?: continue
                        usage.replace(expression)
                    }
                    variable.delete()
                }
            }

            Errors.REDUNDANT_PROJECTION -> { ->
                val fix = RemoveModifierFix.createRemoveProjectionFactory(true).createActions(problem).single() as RemoveModifierFix
                fix.invoke()
            }

            else -> super.fixForProblem(problem)
        }
    }

    private enum class RangeFilterResult {
        SKIP,
        GO_INSIDE,
        PROCESS
    }

    override fun doAdditionalProcessing(file: JetFile, rangeMarker: RangeMarker?) {
        var elementToActions = collectAvailableActions(file, rangeMarker)

        while (elementToActions.isNotEmpty()) {
            val processingsToRerun = HashSet<J2kPostProcessing>()

            for ((element, actions) in elementToActions) {
                for ((action, processing) in actions) {
                    if (element.isValid) {
                        action()
                    }
                    else {
                        processingsToRerun.add(processing)
                    }
                }
            }

            if (processingsToRerun.isEmpty()) break
            //TODO: it looks like there are no such cases currently, add tests later on or drop this
            elementToActions = collectAvailableActions(file, rangeMarker, processingFilter = { it in processingsToRerun })
        }

        if (formatCode) {
            val codeStyleManager = CodeStyleManager.getInstance(file.getProject())
            if (rangeMarker != null) {
                if (rangeMarker.isValid()) {
                    codeStyleManager.reformatRange(file, rangeMarker.getStartOffset(), rangeMarker.getEndOffset())
                }
            }
            else {
                codeStyleManager.reformat(file)
            }
        }
    }

    private data class ActionData(val action: () -> Unit, val processing: J2kPostProcessing)

    private fun collectAvailableActions(
            file: JetFile,
            rangeMarker: RangeMarker?,
            processingFilter: (J2kPostProcessing) -> Boolean = { true }
    ): LinkedHashMap<JetElement, SmartList<ActionData>> {
        val processings = J2KPostProcessingRegistrar.processings.filter(processingFilter)
        val elementToActions = LinkedHashMap<JetElement, SmartList<ActionData>>()

        file.accept(object : PsiRecursiveElementVisitor(){
            override fun visitElement(element: PsiElement) {
                if (element is JetElement) {
                    val rangeResult = rangeFilter(element, rangeMarker)
                    if (rangeResult == RangeFilterResult.SKIP) return

                    super.visitElement(element)

                    if (rangeResult == RangeFilterResult.PROCESS) {
                        processings.forEach { processing ->
                            val action = processing.createAction(element)
                            if (action != null) {
                                elementToActions.getOrPut(element) { SmartList() }.add(ActionData(action, processing))
                            }
                        }
                    }
                }
            }
        })

        return elementToActions
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

    override fun simpleNameReference(nameExpression: JetSimpleNameExpression) = nameExpression.mainReference
}
