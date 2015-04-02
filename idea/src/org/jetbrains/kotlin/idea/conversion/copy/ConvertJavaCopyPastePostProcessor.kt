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
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.*
import org.jetbrains.kotlin.j2k.*
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.idea.editor.JetEditorOptions
import java.awt.datatransfer.Transferable
import com.intellij.openapi.util.TextRange
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.lang.java.JavaLanguage
import org.jetbrains.kotlin.idea.j2k.J2kPostProcessor
import org.jetbrains.kotlin.idea.j2k.IdeaResolverForConverter
import com.intellij.openapi.project.DumbService
import org.jetbrains.kotlin.psi.psiUtil.parents

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

        val sourceFile = PsiFileFactory.getInstance(project).
                createFileFromText(data.fileName, JavaLanguage.INSTANCE, data.fileText) as? PsiJavaFile ?: return

        val targetFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument()) as? JetFile ?: return

        val jetEditorOptions = JetEditorOptions.getInstance()!!
        val needConvert = jetEditorOptions.isEnableJavaToKotlinConversion() && (jetEditorOptions.isDonTShowConversionDialog() || okFromDialog(project))
        if (needConvert) {
            val text = convertCopiedCodeToKotlin(data, sourceFile, data.fileText, targetFile)
            if (!text.isEmpty()) {
                ApplicationManager.getApplication()!!.runWriteAction {
                    val startOffset = bounds.getStartOffset()
                    editor.getDocument().replaceString(startOffset, bounds.getEndOffset(), text)

                    val endOffsetAfterCopy = startOffset + text.length()
                    editor.getCaretModel().moveToOffset(endOffsetAfterCopy)

                    CodeStyleManager.getInstance(project)!!.reformatText(targetFile, startOffset, endOffsetAfterCopy)
                }
            }
        }
    }

    private fun convertCopiedCodeToKotlin(code: CopiedCode, sourceFile: PsiJavaFile, sourceFileText: String, targetFile: JetFile): String {
        val converter = JavaToKotlinConverter(
                sourceFile.getProject(),
                ConverterSettings.defaultSettings,
                FilesConversionScope(listOf(sourceFile)),
                IdeaReferenceSearcher,
                IdeaResolverForConverter
        )
        assert(code.startOffsets.size() == code.endOffsets.size(), "Must have the same size")
        val builder = StringBuilder()
        for (i in code.startOffsets.indices) {
            val textRange = TextRange(code.startOffsets[i], code.endOffsets[i])
            builder.append(convertRangeToKotlin(sourceFile, sourceFileText, targetFile, textRange, converter))
        }
        return StringUtil.convertLineSeparators(builder.toString())
    }

    private fun convertRangeToKotlin(file: PsiJavaFile,
                                     fileText: String,
                                     targetFile: JetFile,
                                     range: TextRange,
                                     converter: JavaToKotlinConverter): String {
        val builder = StringBuilder()
        var currentRange = range
        while (!currentRange.isEmpty()) {
            val leaf = findFirstLeafWhollyInRange(file, currentRange)
            if (leaf == null) {
                val unconvertedSuffix = fileText.substring(currentRange.start, currentRange.end)
                builder.append(unconvertedSuffix)
                break
            }

            val elementToConvert = leaf
                    .parents(withItself = true)
                    .first {
                        val parent = it.getParent()
                        parent == null || parent.range !in currentRange
                    }
            val elementToConvertRange = elementToConvert.range

            val unconvertedPrefix = fileText.substring(currentRange.start, elementToConvertRange.start)
            builder.append(unconvertedPrefix)

            val converted = converter.elementsToKotlin(listOf(elementToConvert to J2kPostProcessor(targetFile, formatCode = false))).single()
            if (!converted.isEmpty()) {
                builder.append(converted)
            }
            else {
                builder.append(fileText.substring(elementToConvertRange.start, elementToConvertRange.end))
            }

            currentRange = TextRange(elementToConvertRange.end, currentRange.end)
        }
        return builder.toString()
    }

    private fun findFirstLeafWhollyInRange(file: PsiJavaFile, range: TextRange): PsiElement? {
        var element = file.findElementAt(range.start) ?: return null
        var elementRange = element.range
        if (elementRange.start < range.start) {
            element = file.findElementAt(elementRange.end) ?: return null
            elementRange = element.range
        }
        assert(elementRange.start >= range.start)
        return if (elementRange.end <= range.end) element else null
    }

    private fun okFromDialog(project: Project): Boolean {
        val dialog = KotlinPasteFromJavaDialog(project)
        dialog.show()
        return dialog.isOK()
    }
}
