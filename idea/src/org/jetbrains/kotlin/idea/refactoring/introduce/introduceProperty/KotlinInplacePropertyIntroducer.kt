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

package org.jetbrains.kotlin.idea.refactoring.introduce.introduceProperty

import com.intellij.codeInsight.template.TemplateBuilderImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Pass
import com.intellij.psi.PsiElement
import com.intellij.ui.NonFocusableCheckBox
import com.intellij.ui.PopupMenuListenerAdapter
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.ExtractionResult
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.ExtractionTarget
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.generateDeclaration
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.processDuplicatesSilently
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceVariable.KotlinInplaceVariableIntroducer
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceVariable.KotlinInplaceVariableIntroducer.ControlWrapper
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.types.KotlinType
import javax.swing.*
import javax.swing.event.PopupMenuEvent

public class KotlinInplacePropertyIntroducer(
        property: KtProperty,
        editor: Editor,
        project: Project,
        title: String,
        doNotChangeVar: Boolean,
        exprType: KotlinType?,
        extractionResult: ExtractionResult,
        private val availableTargets: List<ExtractionTarget>
): KotlinInplaceVariableIntroducer<KtProperty>(
        property, editor, project, title, KtExpression.EMPTY_ARRAY, null, false, property, false, doNotChangeVar, exprType, false
) {
    init {
        assert(availableTargets.isNotEmpty()) { "No targets available: ${property.getElementTextWithContext()}" }
    }

    private var extractionResult = extractionResult

    private var currentTarget: ExtractionTarget = extractionResult.config.generatorOptions.target
        set(value: ExtractionTarget) {
            if (value == currentTarget) return

            field = value
            runWriteActionAndRestartRefactoring {
                with (extractionResult.config) {
                    extractionResult = copy(generatorOptions = generatorOptions.copy(target = currentTarget)).generateDeclaration(property)
                    property = extractionResult.declaration as KtProperty
                    myElementToRename = property
                }
            }
            updatePanelControls()
        }

    private var replaceAll: Boolean = true

    protected var property: KtProperty
        get() = myDeclaration
        set(value: KtProperty) {
            myDeclaration = value
        }

    private fun isInitializer(): Boolean = currentTarget == ExtractionTarget.PROPERTY_WITH_INITIALIZER

    override fun initPanelControls() {
        if (availableTargets.size() > 1) {
            addPanelControl(
                    ControlWrapper {
                        val propertyKindComboBox = with(JComboBox(availableTargets.map { it.targetName.capitalize() }.toTypedArray())) {
                            addPopupMenuListener(
                                    object : PopupMenuListenerAdapter() {
                                        override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent?) {
                                            ApplicationManager.getApplication().invokeLater {
                                                currentTarget = availableTargets[getSelectedIndex()]
                                            }
                                        }
                                    }
                            )

                            setSelectedIndex(availableTargets.indexOf(currentTarget))

                            this
                        }

                        val propertyKindLabel = JLabel("Introduce as: ")
                        propertyKindLabel.setDisplayedMnemonic('I')
                        propertyKindLabel.setLabelFor(propertyKindComboBox)

                        val panel = JPanel()
                        panel.add(propertyKindLabel)
                        panel.add(propertyKindComboBox)

                        panel
                    }
            )
        }

        if (ExtractionTarget.PROPERTY_WITH_INITIALIZER in availableTargets) {
            val condition = { isInitializer() }

            getCreateVarCheckBox()?.let {
                val initializer = object: Pass<JComponent>() {
                    override fun pass(t: JComponent) {
                        (t as JCheckBox).setSelected(property.isVar())
                    }
                }
                addPanelControl(ControlWrapper(it, condition, initializer))
            }
            getCreateExplicitTypeCheckBox()?.let {
                val initializer = object: Pass<JComponent>() {
                    override fun pass(t: JComponent) {
                        (t as JCheckBox).setSelected(property.getTypeReference() != null)
                    }
                }
                addPanelControl(ControlWrapper(it, condition, initializer))
            }
        }

        val occurrenceCount = extractionResult.duplicateReplacers.size() + 1
        if (occurrenceCount > 1) {
            addPanelControl(
                    ControlWrapper {
                        val replaceAllCheckBox = NonFocusableCheckBox("Replace all occurrences ($occurrenceCount)")
                        replaceAllCheckBox.setSelected(replaceAll)
                        replaceAllCheckBox.setMnemonic('R')
                        replaceAllCheckBox.addActionListener { replaceAll = replaceAllCheckBox.isSelected() }
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