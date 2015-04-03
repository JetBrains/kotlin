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

package org.jetbrains.kotlin.idea.refactoring.introduce.introduceParameter

import com.intellij.codeInsight.template.impl.TemplateManagerImpl
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.event.DocumentAdapter
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.impl.DocumentMarkupModel
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.MarkupModel
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.inplace.InplaceRefactoring
import com.intellij.ui.DottedBorder
import com.intellij.ui.JBColor
import org.jetbrains.kotlin.idea.JetFileType
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.refactoring.changeSignature.*
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceVariable.KotlinInplaceVariableIntroducer
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.application.executeCommand
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.psi.JetParameter
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.psi.JetPsiUtil
import org.jetbrains.kotlin.psi.psiUtil.getValueParameterList
import org.jetbrains.kotlin.psi.psiUtil.getValueParameters
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.JetType
import java.awt.BorderLayout
import java.awt.Color
import java.util.ArrayList
import java.util.LinkedHashSet
import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.border.EmptyBorder
import javax.swing.border.LineBorder
import kotlin.properties.Delegates

public class KotlinInplaceParameterIntroducer(
        val descriptor: IntroduceParameterDescriptor,
        editor: Editor,
        project: Project
): KotlinInplaceVariableIntroducer<JetParameter>(
        descriptor.addedParameter,
        editor,
        project,
        INTRODUCE_PARAMETER,
        JetExpression.EMPTY_ARRAY,
        null,
        false,
        descriptor.addedParameter,
        false,
        true,
        descriptor.parameterType,
        false
) {
    companion object {
        private val LOG = Logger.getInstance(javaClass<KotlinInplaceParameterIntroducer>())
    }

    object PreviewDecorator {
        protected val textAttributes: TextAttributes = with(TextAttributes()) {
            setEffectType(EffectType.ROUNDED_BOX)
            setEffectColor(JBColor.RED)
            this
        }

        fun applyToRange(range: TextRange, markupModel: MarkupModel) {
            markupModel.addRangeHighlighter(range.getStartOffset(),
                                            range.getEndOffset(),
                                            0,
                                            textAttributes,
                                            HighlighterTargetArea.EXACT_RANGE
            )
        }
    }

    private var previewer: EditorEx? = null

    private fun updatePreview(currentName: String?, currentType: String?) {
        with (descriptor) {
            var addedRange: TextRange? = null
            val builder = StringBuilder()

            builder.append(callable.getName())

            val parameters = callable.getValueParameters()
            builder.append("(")
            for (i in parameters.indices) {
                val parameter = parameters[i]

                val parameterText = if (parameter == addedParameter){
                    val parameterName = currentName ?: parameter.getName()
                    val parameterType = currentType ?: parameter.getTypeReference()!!.getText()
                    val modifier = if (valVar != JetValVar.None) "${valVar.name} " else ""

                    "$modifier$parameterName: $parameterType"
                }
                else parameter.getText()

                builder.append(parameterText)

                val range = TextRange(builder.length() - parameterText.length(), builder.length())
                if (parameter == addedParameter) {
                    addedRange = range
                }

                if (i < parameters.lastIndex) {
                    builder.append(", ")
                }
            }
            builder.append(")")

            if (addedRange == null) {
                LOG.error("Added parameter not found: ${JetPsiUtil.getElementTextWithContext(callable)}")
            }

            val document = previewer!!.getDocument()
            runWriteAction { document.setText(builder.toString()) }

            val markupModel = DocumentMarkupModel.forDocument(document, myProject, true)
            markupModel.removeAllHighlighters()
            addedRange?.let { PreviewDecorator.applyToRange(it, markupModel) }
        }
        revalidate()
    }

    override fun initPanelControls() {
        addPanelControl {
            val previewer = EditorFactory.getInstance().createEditor(EditorFactory.getInstance().createDocument(""),
                                                                     myProject,
                                                                     JetFileType.INSTANCE,
                                                                     true) as EditorEx
            this.previewer = previewer
            previewer.setOneLineMode(true)

            with(previewer.getSettings()) {
                setAdditionalLinesCount(0)
                setAdditionalColumnsCount(1)
                setRightMarginShown(false)
                setFoldingOutlineShown(false)
                setLineNumbersShown(false)
                setLineMarkerAreaShown(false)
                setIndentGuidesShown(false)
                setVirtualSpace(false)
                setLineCursorWidth(1)
            }

            previewer.setHorizontalScrollbarVisible(false)
            previewer.setVerticalScrollbarVisible(false)
            previewer.setCaretEnabled(false)


            val bg = previewer.getColorsScheme().getColor(EditorColors.CARET_ROW_COLOR)
            previewer.setBackgroundColor(bg)
            previewer.setBorder(BorderFactory.createCompoundBorder(DottedBorder(Color.gray), LineBorder(bg, 2)))

            updatePreview(null, null)

            val previewerPanel = JPanel(BorderLayout())
            previewerPanel.add(previewer.getComponent(), BorderLayout.CENTER)
            previewerPanel.setBorder(EmptyBorder(2, 2, 6, 2))

            previewerPanel
        }
    }

    private var myDocumentAdapter: DocumentAdapter? = null

    fun startRefactoring(suggestedNames: LinkedHashSet<String>): Boolean {
        if (!performInplaceRefactoring(suggestedNames)) return false

        myDocumentAdapter = object : DocumentAdapter() {
            override fun documentChanged(e: DocumentEvent?) {
                if (previewer == null) return
                val templateState = TemplateManagerImpl.getTemplateState(myEditor)
                if (templateState != null) {
                    val name = templateState.getVariableValue(KotlinInplaceVariableIntroducer.PRIMARY_VARIABLE_NAME)?.getText()
                    val typeRefText = templateState.getVariableValue(KotlinInplaceVariableIntroducer.TYPE_REFERENCE_VARIABLE_NAME)?.getText()
                    updatePreview(name, typeRefText)
                }
            }
        }
        myEditor.getDocument().addDocumentListener(myDocumentAdapter!!)

        return true
    }

    override fun finish(success: Boolean) {
        super.finish(success)
        myDocumentAdapter?.let { myEditor.getDocument().removeDocumentListener(it) }
    }

    override fun checkLocalScope(): PsiElement? {
        return descriptor.callable
    }

    override fun performRefactoring(): Boolean {
        descriptor.performRefactoring()
        return true
    }

    override fun performCleanup() {
        runWriteAction { JetPsiUtil.deleteElementWithDelimiters(descriptor.addedParameter) }
    }

    override fun releaseResources() {
        super.releaseResources()
        previewer?.let {
            EditorFactory.getInstance().releaseEditor(it)
            previewer = null
        }
    }
}