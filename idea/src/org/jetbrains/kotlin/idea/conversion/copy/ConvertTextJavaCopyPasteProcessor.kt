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

package org.jetbrains.kotlin.idea.conversion.copy

import com.intellij.codeInsight.editorActions.CopyPastePostProcessor
import com.intellij.codeInsight.editorActions.TextBlockTransferableData
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.LocalTimeCounter
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.editor.KotlinEditorOptions
import org.jetbrains.kotlin.idea.j2k.J2kPostProcessor
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.j2k.AfterConversionPass
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.utils.addToStdlib.check
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable

class CopiedKotlinCodeTransferableData : TextBlockTransferableData {
    override fun getFlavor() = DATA_FLAVOR
    override fun getOffsetCount() = 0

    override fun getOffsets(offsets: IntArray?, index: Int) = index
    override fun setOffsets(offsets: IntArray?, index: Int) = index

    companion object {
        val DATA_FLAVOR: DataFlavor = DataFlavor(CopiedKotlinCodeTransferableData::class.java, "class: KotlinCodeTransferableData")
    }
}

class ConvertTextJavaCopyPasteProcessor : CopyPastePostProcessor<TextBlockTransferableData>() {
    private val LOG = Logger.getInstance(ConvertTextJavaCopyPasteProcessor::class.java)

    private class MyTransferableData(val text: String) : TextBlockTransferableData {

        override fun getFlavor() = DATA_FLAVOR
        override fun getOffsetCount() = 0

        override fun getOffsets(offsets: IntArray?, index: Int) = index
        override fun setOffsets(offsets: IntArray?, index: Int) = index

        companion object {
            val DATA_FLAVOR: DataFlavor = DataFlavor(ConvertTextJavaCopyPasteProcessor::class.java, "class: ConvertTextJavaCopyPasteProcessor")
        }
    }

    override fun collectTransferableData(file: PsiFile, editor: Editor, startOffsets: IntArray, endOffsets: IntArray): List<TextBlockTransferableData> {
        if (file is KtFile) {
            return listOf(CopiedKotlinCodeTransferableData())
        }
        return emptyList()
    }

    override fun extractTransferableData(content: Transferable): List<TextBlockTransferableData> {
        try {
            if (content.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                if (content.isDataFlavorSupported(CopiedJavaCode.DATA_FLAVOR)) return emptyList() // it's handled by ConvertJavaCopyPasteProcessor
                if (content.isDataFlavorSupported(CopiedKotlinCodeTransferableData.DATA_FLAVOR)) return emptyList() // just an optimization

                val text = content.getTransferData(DataFlavor.stringFlavor) as String
                return listOf(MyTransferableData(text))
            }
        }
        catch (e: Throwable) {
            LOG.error(e)
        }
        return emptyList()
    }

    override fun processTransferableData(project: Project, editor: Editor, bounds: RangeMarker, caretOffset: Int, indented: Ref<Boolean>, values: List<TextBlockTransferableData>) {
        if (DumbService.getInstance(project).isDumb) return
        val options = KotlinEditorOptions.getInstance()
        if (!options.isEnableJavaToKotlinConversion) return //TODO: use another option?

        val data = values.single() as MyTransferableData
        val text = data.text

        val psiDocumentManager = PsiDocumentManager.getInstance(project)
        psiDocumentManager.commitDocument(editor.document)
        val targetFile = psiDocumentManager.getPsiFile(editor.document) as? KtFile ?: return

        val pasteContext = detectPasteContext(targetFile, bounds.startOffset, bounds.endOffset) ?: return

        val conversionContext = detectConversionContext(pasteContext, text, project) ?: return

        val needConvert = options.isDonTShowConversionDialog || okFromDialog(project)
        if (!needConvert) return

        val convertedText = convertCodeToKotlin(text, conversionContext, project)

        runWriteAction {
            val startOffset = bounds.startOffset
            editor.document.replaceString(startOffset, bounds.endOffset, convertedText)

            val endOffsetAfterCopy = startOffset + convertedText.length
            editor.caretModel.moveToOffset(endOffsetAfterCopy)

            val newBounds = TextRange(startOffset, startOffset + convertedText.length)

            psiDocumentManager.commitAllDocuments()
            AfterConversionPass(project, J2kPostProcessor(formatCode = true)).run(targetFile, newBounds)

            conversionPerformed = true
        }
    }

    private fun detectPasteContext(file: KtFile, startOffset: Int, endOffset: Int): KotlinContext? {
        if (isNoConversionPosition(file, startOffset)) return null

        val fileText = file.text
        val dummyDeclarationText = "fun dummy(){}"
        val newFileText = fileText.substring(0, startOffset) + "\n" + dummyDeclarationText + "\n" + fileText.substring(endOffset)

        val newFile = parseAsFile(newFileText, KotlinFileType.INSTANCE, file.project)
        val declaration = PsiTreeUtil.findElementOfClassAtRange(newFile, startOffset + 1, startOffset + 1 + dummyDeclarationText.length, KtFunction::class.java) ?: return null
        if (declaration.textLength != dummyDeclarationText.length) return null

        val parent = declaration.parent
        return when (parent) {
            is KtFile -> KotlinContext.TOP_LEVEL
            is KtClassBody -> KotlinContext.CLASS_BODY
            is KtBlockExpression -> KotlinContext.IN_BLOCK
            else -> KotlinContext.EXPRESSION
        }
    }

    private fun detectConversionContext(pasteContext: KotlinContext, text: String, project: Project): JavaContext? {
        if (isParsedAsKotlinCode(text, pasteContext, project)) return null

        fun JavaContext.check(): JavaContext? {
            return check { isParsedAsJavaCode(text, it, project) }
        }

        when (pasteContext) {
            KotlinContext.TOP_LEVEL -> {
                JavaContext.TOP_LEVEL.check()?.let { return it }
                JavaContext.CLASS_BODY.check()?.let { return it }
                return null
            }

            KotlinContext.CLASS_BODY -> return JavaContext.CLASS_BODY.check()

            KotlinContext.IN_BLOCK -> return JavaContext.IN_BLOCK.check()

            KotlinContext.EXPRESSION -> return JavaContext.EXPRESSION.check()
        }
    }

    private enum class KotlinContext {
        TOP_LEVEL, CLASS_BODY, IN_BLOCK, EXPRESSION
    }

    private enum class JavaContext {
        TOP_LEVEL, CLASS_BODY, IN_BLOCK, EXPRESSION
    }

    private fun isParsedAsJavaCode(text: String, context: JavaContext, project: Project): Boolean {
        return when (context) {
            JavaContext.TOP_LEVEL -> isParsedAsJavaFile(text, project)
            JavaContext.CLASS_BODY -> isParsedAsJavaFile("class Dummy { $text\n}", project)
            JavaContext.IN_BLOCK -> isParsedAsJavaFile("class Dummy { void foo() {$text\n}\n}", project)
            JavaContext.EXPRESSION -> isParsedAsJavaFile("class Dummy { Object field = $text; }", project)
        }
    }

    private fun isParsedAsKotlinCode(text: String, context: KotlinContext, project: Project): Boolean {
        return when (context) {
            KotlinContext.TOP_LEVEL -> isParsedAsKotlinFile(text, project)
            KotlinContext.CLASS_BODY -> isParsedAsKotlinFile("class Dummy { $text\n}", project)
            KotlinContext.IN_BLOCK -> isParsedAsKotlinFile("fun foo() {$text\n}", project)
            KotlinContext.EXPRESSION -> isParsedAsKotlinFile("val v = $text", project)
        }
    }

    private fun isParsedAsJavaFile(text: String, project: Project) = isParsedAsFile(text, JavaFileType.INSTANCE, project)

    private fun isParsedAsKotlinFile(text: String, project: Project) = isParsedAsFile(text, KotlinFileType.INSTANCE, project)

    private fun isParsedAsFile(text: String, fileType: LanguageFileType, project: Project): Boolean {
        val psiFile = parseAsFile(text, fileType, project)
        return !psiFile.anyDescendantOfType<PsiErrorElement>()
    }

    private fun parseAsFile(text: String, fileType: LanguageFileType, project: Project): PsiFile {
        return PsiFileFactory.getInstance(project).createFileFromText("Dummy", fileType, text, LocalTimeCounter.currentTime(), false)
    }

    private fun convertCodeToKotlin(text: String, context: JavaContext, project: Project): String {
        val copiedJavaCode = when (context) {
            JavaContext.TOP_LEVEL -> createCopiedJavaCode("$", text)

            JavaContext.CLASS_BODY -> createCopiedJavaCode("class Dummy {\n$\n}", text)

            JavaContext.IN_BLOCK -> createCopiedJavaCode("class Dummy {\nvoid foo() {\n$\n}\n}", text)

            JavaContext.EXPRESSION -> createCopiedJavaCode("class Dummy {\nObject field = $\n}", text)
        }

        val dataForConversion = DataForConversion.prepare(copiedJavaCode, project)
        val conversionResult = convertCopiedCodeToKotlin(dataForConversion.elementsAndTexts, project)
        return conversionResult.text
    }

    private fun createCopiedJavaCode(template: String, text: String): CopiedJavaCode {
        val index = template.indexOf("$")
        assert(index >= 0)
        val fileText = template.substring(0, index) + text + template.substring(index + 1)
        return CopiedJavaCode(fileText, intArrayOf(index), intArrayOf(index + text.length))
    }

    private fun okFromDialog(project: Project): Boolean {
        val dialog = KotlinPasteFromJavaDialog(project, true)
        dialog.show()
        return dialog.isOK
    }

    companion object {
        @TestOnly var conversionPerformed: Boolean = false
    }
}
