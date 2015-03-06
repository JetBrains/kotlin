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

import org.jetbrains.kotlin.idea.refactoring.introduce.introduceVariable.*
import org.jetbrains.kotlin.psi.*
import com.intellij.openapi.project.*
import com.intellij.openapi.editor.*
import org.jetbrains.kotlin.types.*
import javax.swing.*
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.*
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceVariable.KotlinInplaceVariableIntroducer.ControlWrapper
import com.intellij.openapi.util.*
import com.intellij.ui.*
import javax.swing.event.*
import com.intellij.openapi.application.*
import com.intellij.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import com.intellij.codeInsight.template.*
import org.jetbrains.kotlin.idea.intentions.*

public class KotlinInplacePropertyIntroducer(
        property: JetProperty,
        editor: Editor,
        project: Project,
        title: String,
        doNotChangeVar: Boolean,
        exprType: JetType?,
        extractionResult: ExtractionResult,
        private val availableTargets: List<ExtractionTarget>
): KotlinInplaceVariableIntroducer(
        property, editor, project, title, JetExpression.EMPTY_ARRAY, null, false, property, false, doNotChangeVar, exprType, false
) {
    {
        assert(availableTargets.isNotEmpty(), "No targets available: ${JetPsiUtil.getElementTextWithContext(property)}")
    }

    private var extractionResult = extractionResult

    private var currentTarget: ExtractionTarget = extractionResult.config.generatorOptions.target
        set(value: ExtractionTarget) {
            if (value == currentTarget) return

            $currentTarget = value
            runWriteActionAndRestartRefactoring {
                with (extractionResult.config) {
                    extractionResult = copy(generatorOptions = generatorOptions.copy(target = currentTarget)).generateDeclaration(myProperty)
                    myProperty = extractionResult.declaration as JetProperty
                    myElementToRename = myProperty
                }
            }
            updatePanelControls()
        }

    private var replaceAll: Boolean = true

    private fun isInitializer(): Boolean = currentTarget == ExtractionTarget.PROPERTY_WITH_INITIALIZER

    override fun initPanelControls() {
        if (availableTargets.size() > 1) {
            addPanelControl(
                    ControlWrapper {
                        val propertyKindComboBox = with(JComboBox(availableTargets.map { it.name.capitalize() }.copyToArray())) {
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
                        (t as JCheckBox).setSelected(myProperty.isVar())
                    }
                }
                addPanelControl(ControlWrapper(it, condition, initializer))
            }
            getCreateExplicitTypeCheckBox()?.let {
                val initializer = object: Pass<JComponent>() {
                    override fun pass(t: JComponent) {
                        (t as JCheckBox).setSelected(myProperty.getTypeReference() != null)
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
        return myElementToRename.parents().first { it is JetClassOrObject || it is JetFile }
    }

    override fun moveOffsetAfter(success: Boolean) {
        super.moveOffsetAfter(success)
        if (success && replaceAll) {
            processDuplicatesSilently(extractionResult.duplicateReplacers, myProject)
        }
    }
}