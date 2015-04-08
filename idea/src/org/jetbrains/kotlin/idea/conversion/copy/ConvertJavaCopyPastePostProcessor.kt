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
import org.jetbrains.kotlin.idea.codeInsight.KotlinCopyPasteReferenceProcessor
import org.jetbrains.kotlin.idea.codeInsight.KotlinReferenceData
import org.jetbrains.kotlin.idea.editor.JetEditorOptions
import org.jetbrains.kotlin.idea.j2k.IdeaResolverForConverter
import org.jetbrains.kotlin.idea.j2k.J2kPostProcessor
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.j2k.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.elementsInRange
import java.awt.datatransfer.Transferable
import java.util.ArrayList

public class ConvertJavaCopyPastePostProcessor : CopyPastePostProcessor<TextBlockTransferableData>() {
    private val LOG = Logger.getInstance("#org.jetbrains.kotlin.idea.conversion.copy.ConvertJavaCopyPastePostProcessor")

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

    public override fun collectTransferableData(file: PsiFile, editor: Editor, startOffsets: IntArray, endOffsets: IntArray): List<TextBlockTransferableData> {
        if (file !is PsiJavaFile) return listOf()

        return listOf(CopiedJavaCode(file.getText()!!, startOffsets, endOffsets))
    }

    public override fun processTransferableData(project: Project, editor: Editor, bounds: RangeMarker, caretOffset: Int, indented: Ref<Boolean>, values: List<TextBlockTransferableData>) {
        if (DumbService.getInstance(project).isDumb()) return
        val jetEditorOptions = JetEditorOptions.getInstance()
        if (!jetEditorOptions.isEnableJavaToKotlinConversion()) return

        val data = values.single()
        if (data !is CopiedJavaCode) return

        val document = editor.getDocument()
        val targetFile = PsiDocumentManager.getInstance(project).getPsiFile(document) as? JetFile ?: return

        fun doConversion(): Pair<String?, Collection<KotlinReferenceData>> {
            val sourceFile = PsiFileFactory.getInstance(project).createFileFromText(JavaLanguage.INSTANCE, data.fileText) as PsiJavaFile
            val result = convertCopiedCodeToKotlin(data, sourceFile)
            val referenceData = buildReferenceData(result.text, result.parseContext, sourceFile, targetFile)
            return (if (result.textChanged) result.text else null) to referenceData
        }

        fun insertImports(bounds: TextRange, referenceData: Collection<KotlinReferenceData>): TextRange? {
            if (referenceData.isEmpty()) return bounds

            val rangeMarker = document.createRangeMarker(bounds)
            rangeMarker.setGreedyToLeft(true)
            rangeMarker.setGreedyToRight(true)

            KotlinCopyPasteReferenceProcessor().processReferenceData(project, targetFile, bounds.start, referenceData.copyToArray())

            return rangeMarker.range
        }

        var conversionResult: Pair<String?, Collection<KotlinReferenceData>>? = null

        fun doConversionAndInsertImportsIfUnchanged(): Boolean {
            conversionResult = doConversion()

            val text = conversionResult!!.first
            if (text != null) return false

            insertImports(bounds.range ?: return true, conversionResult!!.second)
            return true
        }

        val textLength = data.startOffsets.indices.sumBy { data.endOffsets[it] - data.startOffsets[it] }
        if (textLength < 1000) { // if the text to convert is short enough, try to do conversion without permission from user and skip the dialog if nothing converted
            if (doConversionAndInsertImportsIfUnchanged()) return
        }

        val needConvert = jetEditorOptions.isDonTShowConversionDialog() || okFromDialog(project)
        if (needConvert) {
            if (conversionResult == null) {
                if (doConversionAndInsertImportsIfUnchanged()) return
            }
            val (text, referenceData) = conversionResult!!
            text!! // otherwise we should get true from doConversionAndInsertImportsIfUnchanged and return above

            runWriteAction {
                val startOffset = bounds.getStartOffset()
                document.replaceString(startOffset, bounds.getEndOffset(), text)

                val endOffsetAfterCopy = startOffset + text.length()
                editor.getCaretModel().moveToOffset(endOffsetAfterCopy)

                var newBounds = insertImports(TextRange(startOffset, endOffsetAfterCopy), referenceData)

                PsiDocumentManager.getInstance(project).commitAllDocuments()
                AfterConversionPass(project, J2kPostProcessor(formatCode = true)).run(targetFile, newBounds)

                conversionPerformed = true
            }
        }
    }

    private data class ConversionResult(
            val text: String,
            val parseContext: ParseContext,
            val textChanged: Boolean
    )

    private fun convertCopiedCodeToKotlin(code: CopiedJavaCode, sourceFile: PsiJavaFile): ConversionResult {
        assert(code.startOffsets.size() == code.endOffsets.size(), "Must have the same size")
        val sourceFileText = code.fileText

        val list = ArrayList<Any>()
        for (i in code.startOffsets.indices) {
            list.collectElementsToConvert(sourceFile, sourceFileText, TextRange(code.startOffsets[i], code.endOffsets[i]))
        }

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
        var parseContext: ParseContext? = null
        for (o in list) {
            if (o is PsiElement) {
                val originalText = o.getText()
                originalCodeBuilder.append(originalText)

                val result = results[resultIndex++]
                if (result != null) {
                    convertedCodeBuilder.append(result.text)
                    if (parseContext == null) { // use parse context of the first converted element as parse context for the whole text
                        parseContext = result.parseContext
                    }
                }
                else { // failed to convert element to Kotlin, insert "as is"
                    convertedCodeBuilder.append(originalText)
                }
            }
            else {
                originalCodeBuilder.append(o)
                convertedCodeBuilder.append(o as String)
            }
        }

        val convertedCode = convertedCodeBuilder.toString()
        val originalCode = originalCodeBuilder.toString()
        return ConversionResult(convertedCode, parseContext ?: ParseContext.TOP_LEVEL, convertedCode != originalCode)
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

    private fun buildReferenceData(text: String, parseContext: ParseContext, sourceFile: PsiJavaFile, targetFile: JetFile): Collection<KotlinReferenceData> {
        var blockStart: Int? = null
        var blockEnd: Int? = null
        val fileText = StringBuilder {
            val packageName = sourceFile.getPackageName()
            if (!packageName.isEmpty()) {
                append("package $packageName\n")
            }

            val importList = sourceFile.getImportList()
            if (importList != null) {
                for (import in importList.getImportStatements()) {
                    val qualifiedName = import.getQualifiedName() ?: continue
                    if (import.isOnDemand()) {
                        append("import $qualifiedName.*\n")
                    }
                    else {
                        val fqName = FqNameUnsafe(qualifiedName)
                        // skip explicit imports of platform classes mapped into Kotlin classes
                        if (fqName.isSafe() && JavaToKotlinClassMap.INSTANCE.mapPlatformClass(fqName.toSafe()).isNotEmpty()) continue
                        append("import $qualifiedName\n")
                    }
                }
                //TODO: static imports
            }


            val (contextPrefix, contextSuffix) = when (parseContext) {
                ParseContext.CODE_BLOCK -> "fun ${generateDummyFunctionName(text)}() {\n" to "\n}"
                ParseContext.TOP_LEVEL -> "" to ""
            }

            append(contextPrefix)

            blockStart = length()
            append(text)
            blockEnd = length()

            append(contextSuffix)
        }.toString()

        val dummyFile = JetPsiFactory(targetFile.getProject()).createAnalyzableFile("dummy.kt", fileText, targetFile)

        return KotlinCopyPasteReferenceProcessor().collectReferenceData(dummyFile, intArray(blockStart!!), intArray(blockEnd!!))
    }

    private fun generateDummyFunctionName(convertedCode: String): String {
        var i = 0
        while (true) {
            val name = "dummy$i"
            if (convertedCode.indexOf(name) < 0) return name
            i++
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
