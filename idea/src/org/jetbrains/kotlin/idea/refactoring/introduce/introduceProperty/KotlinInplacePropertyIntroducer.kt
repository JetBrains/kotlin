/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.introduce.introduceProperty

import com.intellij.codeInsight.template.TemplateBuilderImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Pass
import com.intellij.psi.PsiElement
import com.intellij.ui.NonFocusableCheckBox
import com.intellij.ui.PopupMenuListenerAdapter
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.ExtractionResult
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.ExtractionTarget
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.generateDeclaration
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.processDuplicatesSilently
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceVariable.KotlinInplaceVariableIntroducer
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.types.KotlinType
import javax.swing.*
import javax.swing.event.PopupMenuEvent

class KotlinInplacePropertyIntroducer(
    property: KtProperty,
    editor: Editor,
    project: Project,
    title: String,
    doNotChangeVar: Boolean,
    exprType: KotlinType?,
    private var extractionResult: ExtractionResult,
    private val availableTargets: List<ExtractionTarget>
) : KotlinInplaceVariableIntroducer<KtProperty>(
    property, editor, project, title, KtExpression.EMPTY_ARRAY, null, false, property, false, doNotChangeVar, exprType, false
) {
    init {
        assert(availableTargets.isNotEmpty()) { "No targets available: ${property.getElementTextWithContext()}" }
    }

    private var currentTarget: ExtractionTarget = extractionResult.config.generatorOptions.target
        set(value: ExtractionTarget) {
            if (value == currentTarget) return

            field = value
            runWriteActionAndRestartRefactoring {
                with(extractionResult.config) {
                    extractionResult = copy(generatorOptions = generatorOptions.copy(target = currentTarget)).generateDeclaration(property)
                    property = extractionResult.declaration as KtProperty
                    myElementToRename = property
                }
            }
            updatePanelControls()
        }

    private var replaceAll: Boolean = true

    private var property: KtProperty
        get() = myDeclaration
        set(value: KtProperty) {
            myDeclaration = value
        }

    private fun isInitializer(): Boolean = currentTarget == ExtractionTarget.PROPERTY_WITH_INITIALIZER

    override fun initPanelControls() {
        if (availableTargets.size > 1) {
            addPanelControl(
                ControlWrapper {
                    val propertyKindComboBox = with(JComboBox(availableTargets.map { it.targetName.capitalize() }.toTypedArray())) {
                        addPopupMenuListener(
                            object : PopupMenuListenerAdapter() {
                                override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent?) {
                                    ApplicationManager.getApplication().invokeLater {
                                        currentTarget = availableTargets[selectedIndex]
                                    }
                                }
                            }
                        )

                        selectedIndex = availableTargets.indexOf(currentTarget)

                        this
                    }

                    val propertyKindLabel = JLabel(KotlinBundle.message("label.text.introduce.as"))
                    propertyKindLabel.labelFor = propertyKindComboBox

                    val panel = JPanel()
                    panel.add(propertyKindLabel)
                    panel.add(propertyKindComboBox)

                    panel
                }
            )
        }

        if (ExtractionTarget.PROPERTY_WITH_INITIALIZER in availableTargets) {
            val condition = { isInitializer() }

            createVarCheckBox?.let {
                val initializer = object : Pass<JComponent>() {
                    override fun pass(t: JComponent) {
                        (t as JCheckBox).isSelected = property.isVar
                    }
                }
                addPanelControl(ControlWrapper(it, condition, initializer))
            }
            createExplicitTypeCheckBox?.let {
                val initializer = object : Pass<JComponent>() {
                    override fun pass(t: JComponent) {
                        (t as JCheckBox).isSelected = property.typeReference != null
                    }
                }
                addPanelControl(ControlWrapper(it, condition, initializer))
            }
        }

        val occurrenceCount = extractionResult.duplicateReplacers.size
        if (occurrenceCount > 1) {
            addPanelControl(
                ControlWrapper {
                    val replaceAllCheckBox = NonFocusableCheckBox(
                        KotlinBundle.message("checkbox.text.replace.all.occurrences.0", occurrenceCount))
                    replaceAllCheckBox.isSelected = replaceAll
                    replaceAllCheckBox.addActionListener { replaceAll = replaceAllCheckBox.isSelected }
                    replaceAllCheckBox
                }
            )
        }
    }

    override fun addTypeReferenceVariable(builder: TemplateBuilderImpl) {
        if (!isInitializer()) return
        super.addTypeReferenceVariable(builder)
    }

    override fun checkLocalScope(): PsiElement? {
        return myElementToRename.parentsWithSelf.first { it is KtClassOrObject || it is KtFile }
    }

    override fun performRefactoring(): Boolean {
        if (replaceAll) {
            processDuplicatesSilently(extractionResult.duplicateReplacers, myProject)
        }
        return true
    }
}