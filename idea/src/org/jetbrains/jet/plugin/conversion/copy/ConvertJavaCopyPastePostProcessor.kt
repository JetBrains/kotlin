/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.conversion.copy

import com.intellij.codeInsight.editorActions.CopyPastePostProcessor
import com.intellij.codeInsight.editorActions.TextBlockTransferableData
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.*
import org.jetbrains.jet.j2k.*
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.plugin.editor.JetEditorOptions
import java.awt.datatransfer.Transferable
import com.intellij.openapi.util.TextRange
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.codeInsight.editorActions.ReferenceTransferableData

public class ConvertJavaCopyPastePostProcessor() : CopyPastePostProcessor<TextBlockTransferableData>() {

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

        val lightFile = PsiFileFactory.getInstance(file.getProject())!!.createFileFromText(file.getText()!!, file)
        return listOf(CopiedCode(lightFile as? PsiJavaFile, startOffsets, endOffsets))
    }

    public override fun processTransferableData(project: Project, editor: Editor, bounds: RangeMarker, caretOffset: Int, indented: Ref<Boolean>, values: List<TextBlockTransferableData>) {
        assert(values.size() == 1)

        val value = values.first()
        
        if (value !is CopiedCode) return

        val sourceFile = value.getFile() ?: return

        val targetFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument())
        if (targetFile !is JetFile) return

        val jetEditorOptions = JetEditorOptions.getInstance()!!
        val needConvert = jetEditorOptions.isEnableJavaToKotlinConversion() && (jetEditorOptions.isDonTShowConversionDialog() || okFromDialog(project))
        if (needConvert) {
            val text = convertCopiedCodeToKotlin(value, sourceFile)
            if (text.isNotEmpty()) {
                ApplicationManager.getApplication()!!.runWriteAction {
                    val startOffset = bounds.getStartOffset()
                    editor.getDocument().replaceString(bounds.getStartOffset(), bounds.getEndOffset(), text)
                    val endOffsetAfterCopy = startOffset + text.length()
                    editor.getCaretModel().moveToOffset(endOffsetAfterCopy)
                    CodeStyleManager.getInstance(project)!!.reformatText(targetFile, startOffset, endOffsetAfterCopy)
                    PsiDocumentManager.getInstance(targetFile.getProject()).commitDocument(editor.getDocument())
                }
            }
        }
    }

    private fun convertCopiedCodeToKotlin(code: CopiedCode, file: PsiJavaFile): String {
        val converter = Converter.create(file.getProject(), ConverterSettings.defaultSettings, FilesConversionScope(listOf(file)))
        val startOffsets = code.getStartOffsets()
        val endOffsets = code.getEndOffsets()
        assert(startOffsets.size == endOffsets.size) { "Must have the same size" }
        val result = StringBuilder()
        for (i in startOffsets.indices) {
            val startOffset = startOffsets[i]
            val endOffset = endOffsets[i]
            result.append(convertRangeToKotlin(file, TextRange(startOffset, endOffset), converter))
        }
        return StringUtil.convertLineSeparators(result.toString())
    }

    private fun convertRangeToKotlin(file: PsiJavaFile,
                                     range: TextRange,
                                     converter: Converter): String {
        val result = StringBuilder()
        var currentRange = range
        //TODO: probably better to use document to get text by range
        val fileText = file.getText()!!
        while (!currentRange.isEmpty()) {
            val leafElement = findFirstLeafElementWhollyInRange(file, currentRange)
            if (leafElement == null) {
                val unconvertedSuffix = fileText.substring(currentRange.start, currentRange.end)
                result.append(unconvertedSuffix)
                break
            }
            val elementToConvert = findTopMostParentWhollyInRange(currentRange, leafElement)
            val unconvertedPrefix = fileText.substring(currentRange.start, elementToConvert.range.start)
            result.append(unconvertedPrefix)
            val converted = converter.elementToKotlin(elementToConvert)
            if (converted.isNotEmpty()) {
                result.append(converted)
            }
            else {
                result.append(elementToConvert.getText())
            }
            val endOfConverted = elementToConvert.range.end
            currentRange = TextRange(endOfConverted, currentRange.end)
        }
        return result.toString()
    }

    private fun findFirstLeafElementWhollyInRange(file: PsiJavaFile, range: TextRange): PsiElement? {
        var i = range.start
        while (i < range.end) {
            val element = file.findElementAt(i)
            if (element == null) {
                ++i
                continue
            }
            val elemRange = element.range
            if (elemRange !in range) {
                i = elemRange.end
                continue
            }
            return element
        }
        return null
    }

    private fun findTopMostParentWhollyInRange(range: TextRange,
                                               base: PsiElement): PsiElement {
        assert(base.range in range) {
            "Base element out of range. Range: $range, element: ${base.getText()}, element's range: ${base.range}."
        }
        var elem = base
        while (true) {
            val parent = elem.getParent()
            if (parent == null || parent is PsiJavaFile || parent.range !in range) {
                break
            }
            elem = parent
        }
        return elem
    }

    private fun okFromDialog(project: Project): Boolean {
        val dialog = KotlinPasteFromJavaDialog(project)
        dialog.show()
        return dialog.isOK()
    }

    class object {
        private val LOG = Logger.getInstance("#org.jetbrains.jet.plugin.conversion.copy.ConvertJavaCopyPastePostProcessor")!!
    }
}
