/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.project.platform
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.js.isJs
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.quoteIfNeeded
import org.jetbrains.kotlin.util.capitalizeDecapitalize.decapitalizeSmart
import org.jetbrains.kotlin.utils.SmartList

sealed class ConvertTestFunctionToSpacedIntention(case: String) : SelfTargetingRangeIntention<KtNamedFunction>(
    KtNamedFunction::class.java, KotlinBundle.lazyMessage("replace.0.name.with.spaces", case)
) {
    companion object {
        private val SNAKE_CASE_REGEX = ".+_.+".toRegex()
    }

    abstract fun split(name: String): List<String>

    abstract fun isApplicableName(name: String): Boolean

    protected fun isSnakeCase(name: String): Boolean = name.contains(SNAKE_CASE_REGEX)

    override fun applicabilityRange(element: KtNamedFunction): TextRange? {
        val platform = element.platform
        if (platform.isCommon() || platform.isJs()) return null
        val range = element.nameIdentifier?.textRange ?: return null

        val name = element.name ?: return null
        if (!isApplicableName(name)) return null
        val newName = convert(name)
        if (newName == name.quoteIfNeeded()) return null

        val lightMethod = element.toLightMethods().firstOrNull() ?: return null
        if (!TestFrameworks.getInstance().isTestMethod(lightMethod)) return null

        setTextGetter(KotlinBundle.lazyMessage("rename.to.01", newName))

        return range
    }

    private fun convert(name: String) = split(name).joinToString(separator = " ") { it.decapitalizeSmart().trim() }.quoteIfNeeded()

    private fun startRename(element: KtNamedFunction, newName: String) {
        RenameProcessor(element.project, element, newName, false, false).run()
    }

    override fun startInWriteAction() = false

    override fun applyTo(element: KtNamedFunction, editor: Editor?) {
        val nameIdentifier = element.nameIdentifier ?: return
        val oldName = element.name ?: return
        val oldId = oldName.quoteIfNeeded()
        val newId = convert(oldName)

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
                        chosenId = state.getVariableValue(varName)?.text?.quoteIfNeeded() ?: return
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
        } else {
            startRename(element, newId)
        }
    }
}


class ConvertCamelCaseTestFunctionToSpacedIntention : ConvertTestFunctionToSpacedIntention("camel-case") {
    override fun isApplicableName(name: String): Boolean {
        return !isSnakeCase(name)
    }

    enum class Case {
        LOWER, UPPER, OTHER
    }

    override fun split(name: String): List<String> {
        if (name === "") return emptyList()

        val result = SmartList<String>()
        var previousCase = Case.OTHER
        var from = 0
        for (i in name.indices) {
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
}

class ConvertSnakeCaseTestFunctionToSpacedIntention : ConvertTestFunctionToSpacedIntention("snake_case") {
    override fun isApplicableName(name: String): Boolean {
        return isSnakeCase(name)
    }

    override fun split(name: String): List<String> {
        return name.split("_").filter { it.isNotBlank() }
    }
}