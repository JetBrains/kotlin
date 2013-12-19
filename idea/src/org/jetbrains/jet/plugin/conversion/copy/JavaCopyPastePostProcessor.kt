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
import java.util.ArrayList

public class JavaCopyPastePostProcessor() : CopyPastePostProcessor<TextBlockTransferableData> {

    override fun extractTransferableData(content: Transferable?): TextBlockTransferableData? {
        try {
            if (content!!.isDataFlavorSupported(CopiedCode.DATA_FLAVOR)) {
                return (content.getTransferData(CopiedCode.DATA_FLAVOR) as TextBlockTransferableData)
            }
        }
        catch (e: Throwable) {
            LOG.error(e)
        }
        return null
    }

    public override fun collectTransferableData(file: PsiFile?, editor: Editor?, startOffsets: IntArray?, endOffsets: IntArray?): TextBlockTransferableData? {
        if (!(file is PsiJavaFile)) {
            return null
        }

        val lightFile = PsiFileFactory.getInstance(file.getProject())!!.createFileFromText(file.getText()!!, file)
        return CopiedCode(lightFile, startOffsets!!, endOffsets!!)
    }

    public override fun processTransferableData(project: Project?, editor: Editor?, bounds: RangeMarker?,
                                                caretOffset: Int, indented: Ref<Boolean>?, value: TextBlockTransferableData?) {
        if (value !is CopiedCode)
            return

        val file = PsiDocumentManager.getInstance(project!!).getPsiFile(editor!!.getDocument())
        if (file !is JetFile)
            return

        val jetEditorOptions = JetEditorOptions.getInstance()!!
        val needConvert = jetEditorOptions.isEnableJavaToKotlinConversion() && (jetEditorOptions.isDonTShowConversionDialog() || okFromDialog(project))
        if (needConvert) {
            val text = convertCopiedCodeToKotlin((value as CopiedCode), file)
            if (text.isNotEmpty() ) {
                ApplicationManager.getApplication()!!.runWriteAction {
                    editor.getDocument().replaceString(bounds!!.getStartOffset(), bounds.getEndOffset(), text)
                    editor.getCaretModel().moveToOffset(bounds.getStartOffset() + text.length())
                    PsiDocumentManager.getInstance(file.getProject()).commitDocument(editor.getDocument())
                }
            }
        }
    }

    private fun convertCopiedCodeToKotlin(code: CopiedCode, file: PsiFile): String {
        val buffer = getSelectedElements(code.getFile()!!, code.getStartOffsets(), code.getEndOffsets())
        val project = file.getProject()
        val converter = Converter(project, PluginSettings)
        val result = buffer.map { converter.elementToKotlin(it) }.makeString("")
        return StringUtil.convertLineSeparators(result.toString())
    }

    private fun getSelectedElements(file: PsiFile, startOffsets: IntArray, endOffsets: IntArray): MutableList<PsiElement> {
        val buffer = ArrayList<PsiElement>()
        assert(startOffsets.size == endOffsets.size) { "Must have the same size" }
        for (i in 0..startOffsets.size - 1) {
            val startOffset = startOffsets[i]
            val endOffset = endOffsets[i]
            var elem = file.findElementAt(startOffset)
            while (elem != null &&
                elem!!.getParent() != null &&
                !(elem!!.getParent() is PsiFile) &&
                elem!!.getParent()!!.getTextRange()!!.getEndOffset() <= endOffset) {
                elem = elem!!.getParent()
            }
            if (elem != null) {
                buffer.add(elem!!)
            }
            while (elem != null && elem!!.getTextRange()!!.getEndOffset() < endOffset) {
                elem = elem!!.getNextSibling()
                if (elem != null) {
                    buffer.add(elem!!)
                }
            }
        }
        return buffer
    }

    private fun okFromDialog(project: Project): Boolean {
        val dialog = KotlinPasteFromJavaDialog(project)
        dialog.show()
        return dialog.isOK()
    }

    class object {
        private val LOG = Logger.getInstance("#org.jetbrains.jet.plugin.conversion.copy.JavaCopyPastePostProcessor")!!
        private val EOL = System.getProperty("line.separator")!!
    }
}
