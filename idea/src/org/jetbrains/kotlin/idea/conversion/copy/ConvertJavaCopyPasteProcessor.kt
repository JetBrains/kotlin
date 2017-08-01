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
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.psi.*
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.idea.actions.JavaToKotlinAction
import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.codeInsight.KotlinCopyPasteReferenceProcessor
import org.jetbrains.kotlin.idea.codeInsight.KotlinReferenceData
import org.jetbrains.kotlin.idea.editor.KotlinEditorOptions
import org.jetbrains.kotlin.idea.j2k.IdeaJavaToKotlinServices
import org.jetbrains.kotlin.idea.j2k.J2kPostProcessor
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.j2k.AfterConversionPass
import org.jetbrains.kotlin.j2k.ConverterSettings
import org.jetbrains.kotlin.j2k.JavaToKotlinConverter
import org.jetbrains.kotlin.j2k.ParseContext
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtStringTemplateEntryWithExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import java.awt.datatransfer.Transferable
import java.util.*

class ConvertJavaCopyPasteProcessor : CopyPastePostProcessor<TextBlockTransferableData>() {
    private val LOG = Logger.getInstance(ConvertJavaCopyPasteProcessor::class.java)

    override fun extractTransferableData(content: Transferable): List<TextBlockTransferableData> {
        try {
            if (content.isDataFlavorSupported(CopiedJavaCode.DATA_FLAVOR)) {
                return listOf(content.getTransferData(CopiedJavaCode.DATA_FLAVOR) as TextBlockTransferableData)
            }
        }
        catch (e: Throwable) {
            LOG.error(e)
        }
        return listOf()
    }

    override fun collectTransferableData(file: PsiFile, editor: Editor, startOffsets: IntArray, endOffsets: IntArray): List<TextBlockTransferableData> {
        if (file !is PsiJavaFile) return listOf()

        return listOf(CopiedJavaCode(file.getText()!!, startOffsets, endOffsets))
    }

    override fun processTransferableData(project: Project, editor: Editor, bounds: RangeMarker, caretOffset: Int, indented: Ref<Boolean>, values: List<TextBlockTransferableData>) {
        if (DumbService.getInstance(project).isDumb) return
        if (!KotlinEditorOptions.getInstance().isEnableJavaToKotlinConversion) return

        val data = values.single() as CopiedJavaCode

        val document = editor.document
        val targetFile = PsiDocumentManager.getInstance(project).getPsiFile(document) as? KtFile ?: return

        if (isNoConversionPosition(targetFile, bounds.startOffset)) return

        data class Result(val text: String?, val referenceData: Collection<KotlinReferenceData>, val explicitImports: Set<FqName>)

        fun doConversion(): Result {
            val dataForConversion = DataForConversion.prepare(data, project)
            val result = dataForConversion.elementsAndTexts.convertCodeToKotlin(project)
            val referenceData = buildReferenceData(result.text, result.parseContext, dataForConversion.importsAndPackage, targetFile)
            val text = if (result.textChanged) result.text else null
            return Result(text, referenceData, result.importsToAdd)
        }

        fun insertImports(bounds: TextRange, referenceData: Collection<KotlinReferenceData>, explicitImports: Collection<FqName>): TextRange? {
            if (referenceData.isEmpty() && explicitImports.isEmpty()) return bounds

            PsiDocumentManager.getInstance(project).commitAllDocuments()

            val rangeMarker = document.createRangeMarker(bounds)
            rangeMarker.isGreedyToLeft = true
            rangeMarker.isGreedyToRight = true

            KotlinCopyPasteReferenceProcessor().processReferenceData(project, targetFile, bounds.start, referenceData.toTypedArray())

            runWriteAction {
                explicitImports.forEach { fqName ->
                    targetFile.resolveImportReference(fqName).firstOrNull()?.let {
                        ImportInsertHelper.getInstance(project).importDescriptor(targetFile, it)
                    }
                }
            }

            return rangeMarker.range
        }

        var conversionResult: Result? = null

        fun doConversionAndInsertImportsIfUnchanged(): Boolean {
            conversionResult = doConversion()

            if (conversionResult!!.text != null) return false

            insertImports(bounds.range ?: return true, conversionResult!!.referenceData, conversionResult!!.explicitImports)
            return true
        }

        val textLength = data.startOffsets.indices.sumBy { data.endOffsets[it] - data.startOffsets[it] }
        if (textLength < 1000) { // if the text to convert is short enough, try to do conversion without permission from user and skip the dialog if nothing converted
            if (doConversionAndInsertImportsIfUnchanged()) return
        }

        if (confirmConvertJavaOnPaste(project, isPlainText = false)) {
            if (conversionResult == null) {
                if (doConversionAndInsertImportsIfUnchanged()) return
            }
            val (text, referenceData, explicitImports) = conversionResult!!
            text!! // otherwise we should get true from doConversionAndInsertImportsIfUnchanged and return above

            val boundsAfterReplace =
                    runWriteAction {
                        val startOffset = bounds.startOffset
                        document.replaceString(startOffset, bounds.endOffset, text)

                        val endOffsetAfterCopy = startOffset + text.length
                        editor.caretModel.moveToOffset(endOffsetAfterCopy)
                        TextRange(startOffset, endOffsetAfterCopy)
                    }

            val newBounds = insertImports(boundsAfterReplace, referenceData, explicitImports)

            PsiDocumentManager.getInstance(project).commitAllDocuments()
            AfterConversionPass(project, J2kPostProcessor(formatCode = true)).run(targetFile, newBounds)

            conversionPerformed = true
        }
    }

    private fun buildReferenceData(text: String, parseContext: ParseContext, importsAndPackage: String, targetFile: KtFile): Collection<KotlinReferenceData> {
        var blockStart: Int? = null
        var blockEnd: Int? = null
        val fileText = buildString {
            append(importsAndPackage)

            val (contextPrefix, contextSuffix) = when (parseContext) {
                ParseContext.CODE_BLOCK -> "fun ${generateDummyFunctionName(text)}() {\n" to "\n}"
                ParseContext.TOP_LEVEL -> "" to ""
            }

            append(contextPrefix)

            blockStart = length
            append(text)
            blockEnd = length

            append(contextSuffix)
        }

        val dummyFile = KtPsiFactory(targetFile.project).createAnalyzableFile("dummy.kt", fileText, targetFile)

        return KotlinCopyPasteReferenceProcessor().collectReferenceData(dummyFile, intArrayOf(blockStart!!), intArrayOf(blockEnd!!))
    }

    private fun generateDummyFunctionName(convertedCode: String): String {
        var i = 0
        while (true) {
            val name = "dummy$i"
            if (convertedCode.indexOf(name) < 0) return name
            i++
        }
    }

    companion object {
        @TestOnly var conversionPerformed: Boolean = false
    }
}

internal class ConversionResult(
        val text: String,
        val parseContext: ParseContext,
        val importsToAdd: Set<FqName>,
        val textChanged: Boolean
)

internal fun ElementAndTextList.convertCodeToKotlin(project: Project): ConversionResult {
    val converter = JavaToKotlinConverter(
            project,
            ConverterSettings.defaultSettings,
            IdeaJavaToKotlinServices
    )

    val inputElements = this.toList().filterIsInstance<PsiElement>()
    val results =
            ProgressManager.getInstance().runProcessWithProgressSynchronously(
                    ThrowableComputable<JavaToKotlinConverter.Result, Exception> {
                        runReadAction { converter.elementsToKotlin(inputElements) }
                    },
                    JavaToKotlinAction.title,
                    false,
                    project
            ).results


    val importsToAdd = LinkedHashSet<FqName>()

    var resultIndex = 0
    val convertedCodeBuilder = StringBuilder()
    val originalCodeBuilder = StringBuilder()
    var parseContext: ParseContext? = null
    this.process(object : ElementsAndTextsProcessor {
        override fun processElement(element: PsiElement) {
            val originalText = element.text
            originalCodeBuilder.append(originalText)

            val result = results[resultIndex++]
            if (result != null) {
                convertedCodeBuilder.append(result.text)
                if (parseContext == null) { // use parse context of the first converted element as parse context for the whole text
                    parseContext = result.parseContext
                }
                importsToAdd.addAll(result.importsToAdd)
            }
            else { // failed to convert element to Kotlin, insert "as is"
                convertedCodeBuilder.append(originalText)
            }
        }

        override fun processText(string: String) {
            originalCodeBuilder.append(string)
            convertedCodeBuilder.append(string)
        }
    })

    val convertedCode = convertedCodeBuilder.toString()
    val originalCode = originalCodeBuilder.toString()
    return ConversionResult(convertedCode, parseContext ?: ParseContext.CODE_BLOCK, importsToAdd, convertedCode != originalCode)
}

internal fun isNoConversionPosition(file: KtFile, offset: Int): Boolean {
    if (offset == 0) return false
    val token = file.findElementAt(offset - 1)!!

    if (token !is PsiWhiteSpace && token.endOffset != offset) return true // pasting into the middle of token

    for (element in token.parentsWithSelf) {
        if (element is PsiComment) {
            return element.node.elementType == KtTokens.EOL_COMMENT || offset != element.endOffset
        }
        if (element is KtStringTemplateEntryWithExpression) return false
        if (element is KtStringTemplateExpression) return true
    }
    return false
}

internal fun confirmConvertJavaOnPaste(project: Project, isPlainText: Boolean): Boolean {
    if (KotlinEditorOptions.getInstance().isDonTShowConversionDialog) return true

    val dialog = KotlinPasteFromJavaDialog(project, isPlainText)
    dialog.show()
    return dialog.isOK
}

