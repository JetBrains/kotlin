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

package org.jetbrains.kotlin.idea.conversion.copy

import com.intellij.codeInsight.editorActions.CopyPastePostProcessor
import com.intellij.codeInsight.editorActions.TextBlockTransferableData
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.kotlin.idea.editor.JetEditorOptions
import org.jetbrains.kotlin.idea.j2k.IdeaResolverForConverter
import org.jetbrains.kotlin.idea.j2k.J2kPostProcessor
import org.jetbrains.kotlin.j2k.AfterConversionPass
import org.jetbrains.kotlin.j2k.ConverterSettings
import org.jetbrains.kotlin.j2k.IdeaReferenceSearcher
import org.jetbrains.kotlin.j2k.JavaToKotlinConverter
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.psiUtil.elementsInRange
import org.jetbrains.kotlin.psi.psiUtil.parents
import java.awt.datatransfer.Transferable
import java.util.ArrayList

public class ConvertJavaCopyPastePostProcessor : CopyPastePostProcessor<TextBlockTransferableData>() {
    private val LOG = Logger.getInstance("#org.jetbrains.kotlin.idea.conversion.copy.ConvertJavaCopyPastePostProcessor")

    override fun extractTransferableData(content: Transferable): List<TextBlockTransferableData> {
        try {
            if (content.isDataFlavorSupported(CopiedCode.DATA_FLAVOR)) {
                return listOf(content.getTransferData(CopiedCode.DATA_FLAVOR) as TextBlockTransferableData)
            }
        }
        catch (e: Throwable) {
            LOG.error(e)
        }
        return listOf()
    }

    public override fun collectTransferableData(file: PsiFile, editor: Editor, startOffsets: IntArray, endOffsets: IntArray): List<TextBlockTransferableData> {
        if (file !is PsiJavaFile) return listOf()

        return listOf(CopiedCode(file.getName(), file.getText()!!, startOffsets, endOffsets))
    }

    public override fun processTransferableData(project: Project, editor: Editor, bounds: RangeMarker, caretOffset: Int, indented: Ref<Boolean>, values: List<TextBlockTransferableData>) {
        if (DumbService.getInstance(project).isDumb()) return

        val data = values.single()
        if (data !is CopiedCode) return

        val targetFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument()) as? JetFile ?: return

        fun doConversion(): String? {
            val sourceFile = PsiFileFactory.getInstance(project).
                    createFileFromText(data.fileName, JavaLanguage.INSTANCE, data.fileText) as PsiJavaFile
            return convertCopiedCodeToKotlin(data, sourceFile)
        }

        var conversionResult: String? = null

        val textLength = data.startOffsets.indices.sumBy { data.endOffsets[it] - data.startOffsets[it] }
        if (textLength < 1000) { // if the text to convert is short enough, try to do conversion without permission from user and skip the dialog if nothing converted
            conversionResult = doConversion() ?: return
        }

        val jetEditorOptions = JetEditorOptions.getInstance()!!
        val needConvert = jetEditorOptions.isEnableJavaToKotlinConversion() && (jetEditorOptions.isDonTShowConversionDialog() || okFromDialog(project))
        if (needConvert) {
            if (conversionResult == null) {
                conversionResult = doConversion() ?: return
            }
            ApplicationManager.getApplication()!!.runWriteAction {
                val startOffset = bounds.getStartOffset()
                editor.getDocument().replaceString(startOffset, bounds.getEndOffset(), conversionResult!!)

                val endOffsetAfterCopy = startOffset + conversionResult!!.length()
                editor.getCaretModel().moveToOffset(endOffsetAfterCopy)

                PsiDocumentManager.getInstance(project).commitAllDocuments()
                AfterConversionPass(project, J2kPostProcessor(formatCode = true)).run(targetFile, TextRange(startOffset, endOffsetAfterCopy))

                conversionPerformed = true
            }
        }
    }

    private fun convertCopiedCodeToKotlin(code: CopiedCode, sourceFile: PsiJavaFile): String? {
        assert(code.startOffsets.size() == code.endOffsets.size(), "Must have the same size")
        val sourceFileText = code.fileText

        val list = ArrayList<Any>()
        for (i in code.startOffsets.indices) {
            list.collectElementsToConvert(sourceFile, sourceFileText, TextRange(code.startOffsets[i], code.endOffsets[i]))
        }

        if (list.all { it is String }) return null // nothing to convert

        val converter = JavaToKotlinConverter(
                sourceFile.getProject(),
                ConverterSettings.defaultSettings,
                IdeaReferenceSearcher,
                IdeaResolverForConverter,
                null
        )

        val inputElements = list.filterIsInstance<PsiElement>().map { JavaToKotlinConverter.InputElement(it, null) }
        val results = converter.elementsToKotlin(inputElements)

        var resultIndex = 0
        val convertedCodeBuilder = StringBuilder()
        val originalCodeBuilder = StringBuilder()
        for (o in list) {
            if (o is PsiElement) {
                val originalText = o.getText()
                val result = results[resultIndex++]
                if (!result.isEmpty()) {
                    convertedCodeBuilder.append(result)
                }
                else { // failed to convert element to Kotlin, insert "as is"
                    convertedCodeBuilder.append(originalText)
                }
                originalCodeBuilder.append(originalText)
            }
            else {
                convertedCodeBuilder.append(o as String)
                originalCodeBuilder.append(o)
            }
        }

        val convertedCode = convertedCodeBuilder.toString()
        val originalCode = originalCodeBuilder.toString()
        if (convertedCode == originalCode) return null

        return convertedCode
    }

    // builds list consisting of PsiElement's to convert and plain String's
    private fun MutableList<Any>.collectElementsToConvert(
            file: PsiJavaFile,
            fileText: String,
            range: TextRange
    ) {
        val elements = file.elementsInRange(range)
        if (elements.isEmpty()) {
            add(fileText.substring(range.getStartOffset(), range.getEndOffset()))
        }
        else {
            add(fileText.substring(range.getStartOffset(), elements.first().getTextRange().getStartOffset()))
            addAll(elements)
            add(fileText.substring(elements.last().getTextRange().getEndOffset(), range.getEndOffset()))
        }
    }

    private fun okFromDialog(project: Project): Boolean {
        val dialog = KotlinPasteFromJavaDialog(project)
        dialog.show()
        return dialog.isOK()
    }

    companion object {
        // used for testing
        public var conversionPerformed: Boolean = false
    }
}
