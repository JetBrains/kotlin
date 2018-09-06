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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.TestFrameworks
import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.TemplateBuilderImpl
import com.intellij.codeInsight.template.TemplateEditingAdapter
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInsight.template.impl.TemplateImpl
import com.intellij.codeInsight.template.impl.TemplateState
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.refactoring.rename.RenameProcessor
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.idea.project.platform
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.js.resolve.JsPlatform
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.quoteIfNeeded
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.util.capitalizeDecapitalize.decapitalizeSmart
import org.jetbrains.kotlin.utils.SmartList

class ConvertCamelCaseTestFunctionToSpacedIntention : SelfTargetingRangeIntention<KtNamedFunction>(
        KtNamedFunction::class.java, "Replace camel-case name with spaces"
) {
    override fun applicabilityRange(element: KtNamedFunction): TextRange? {
        val platform = element.platform
        if (platform == TargetPlatform.Common || platform == JsPlatform) return null
        val range = element.nameIdentifier?.textRange ?: return null

        val name = element.name ?: return null
        val newName = decamelize(name)
        if (newName == name.quoteIfNeeded()) return null

        val lightMethod = element.toLightMethods().firstOrNull() ?: return null
        if (!TestFrameworks.getInstance().isTestMethod(lightMethod)) return null

        text = "Rename to $newName"

        return range
    }

    enum class Case {
        LOWER, UPPER, OTHER
    }

    private fun splitCamelName(name: String): List<String> {
        if (name === "") return emptyList()

        val result = SmartList<String>()
        var previousCase = Case.OTHER
        var from = 0
        for (i in 0 until name.length) {
            val c = name[i]
            val currentCase = when {
                Character.isUpperCase(c) -> Case.UPPER
                Character.isLowerCase(c) -> Case.LOWER
                else -> Case.OTHER
            }

            when {
                i == name.lastIndex -> result += name.substring(from)
                i > 0 && currentCase != previousCase && currentCase != Case.LOWER -> {
                    result += name.substring(from, i)
                    from = i
                }
            }

            previousCase = currentCase
        }

        return result
    }

    private fun decamelize(name: String) = splitCamelName(name).joinToString(separator = " ") { it.decapitalizeSmart().trim() }.quoteIfNeeded()

    private fun startRename(element: KtNamedFunction, newName: String) {
        RenameProcessor(element.project, element, newName, false, false).run()
    }

    override fun startInWriteAction() = false

    override fun applyTo(element: KtNamedFunction, editor: Editor?) {
        val nameIdentifier = element.nameIdentifier ?: return
        val oldName = element.name ?: return
        val oldId = oldName.quoteIfNeeded()
        val newId = decamelize(oldName)

        if (editor != null) {
            val builder = TemplateBuilderImpl(nameIdentifier)
            builder.replaceElement(nameIdentifier, newId)
            val template = runWriteAction { builder.buildInlineTemplate() }
            TemplateManager.getInstance(element.project).startTemplate(
                    editor,
                    template,
                    object : TemplateEditingAdapter() {
                        private var chosenId: String = newId
                        private var range: TextRange? = null

                        override fun beforeTemplateFinished(state: TemplateState, template: Template?) {
                            val varName = (template as? TemplateImpl)?.getVariableNameAt(0) ?: return
                            chosenId = state?.getVariableValue(varName)?.text?.quoteIfNeeded() ?: return
                            range = state.getVariableRange(varName)
                        }

                        override fun templateFinished(template: Template, brokenOff: Boolean) {
                            range?.let {
                                val doc = editor.document
                                runWriteAction { doc.replaceString(it.startOffset, it.endOffset, oldId) }
                                PsiDocumentManager.getInstance(element.project).commitDocument(doc)
                            }

                            if (!brokenOff && chosenId != oldId) {
                                startRename(element, chosenId)
                            }
                        }
                    }
            )
        }
        else {
            startRename(element, newId)
        }
    }
}