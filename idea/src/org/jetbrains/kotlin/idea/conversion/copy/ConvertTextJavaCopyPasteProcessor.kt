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
import com.intellij.codeInsight.editorActions.TextBlockTransferable
import com.intellij.codeInsight.editorActions.TextBlockTransferableData
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.psi.*
import com.intellij.util.LocalTimeCounter
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.core.util.range
import org.jetbrains.kotlin.idea.editor.KotlinEditorOptions
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.idea.util.projectStructure.module
import org.jetbrains.kotlin.j2k.ConversionType
import org.jetbrains.kotlin.j2k.J2kConverterExtension
import org.jetbrains.kotlin.j2k.logJ2kConversionStatistics
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import kotlin.system.measureTimeMillis

class ConvertTextJavaCopyPasteProcessor : CopyPastePostProcessor<TextBlockTransferableData>() {
    private val LOG = Logger.getInstance(ConvertTextJavaCopyPasteProcessor::class.java)
    private val javaContextDeclarationRenderer = JavaContextDeclarationRenderer()

    private class MyTransferableData(val text: String) : TextBlockTransferableData {

        override fun getFlavor() = DATA_FLAVOR
        override fun getOffsetCount() = 0

        override fun getOffsets(offsets: IntArray?, index: Int) = index
        override fun setOffsets(offsets: IntArray?, index: Int) = index

        companion object {
            val DATA_FLAVOR: DataFlavor =
                DataFlavor(ConvertTextJavaCopyPasteProcessor::class.java, "class: ConvertTextJavaCopyPasteProcessor")
        }
    }

    override fun collectTransferableData(
        file: PsiFile,
        editor: Editor,
        startOffsets: IntArray,
        endOffsets: IntArray
    ): List<TextBlockTransferableData> {
        if (file is KtFile) return listOf(CopiedKotlinCode(file.text, startOffsets, endOffsets))
        return emptyList()
    }

    override fun extractTransferableData(content: Transferable): List<TextBlockTransferableData> {
        try {
            if (content.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                if (content.isDataFlavorSupported(CopiedKotlinCode.DATA_FLAVOR) ||
                    /* Handled by ConvertJavaCopyPasteProcessor */
                    content.isDataFlavorSupported(CopiedJavaCode.DATA_FLAVOR)
                ) return emptyList()

                val text = content.getTransferData(DataFlavor.stringFlavor) as String
                return listOf(MyTransferableData(text))
            }
        } catch (e: Throwable) {
            if (e is ControlFlowException) throw e
            LOG.error(e)
        }
        return emptyList()
    }

    override fun processTransferableData(
        project: Project,
        editor: Editor,
        bounds: RangeMarker,
        caretOffset: Int,
        indented: Ref<Boolean>,
        values: List<TextBlockTransferableData>
    ) {
        if (DumbService.getInstance(project).isDumb) return
        if (!KotlinEditorOptions.getInstance().isEnableJavaToKotlinConversion) return //TODO: use another option?

        val text = TextBlockTransferable.convertLineSeparators(editor, (values.single() as MyTransferableData).text, values)

        val psiDocumentManager = PsiDocumentManager.getInstance(project)
        val targetFile = psiDocumentManager.getPsiFile(editor.document) as? KtFile ?: return
        psiDocumentManager.commitDocument(editor.document)

        val useNewJ2k = checkUseNewJ2k(targetFile)
        val targetModule = targetFile.module
        val pasteTarget = detectPasteTarget(targetFile, bounds.startOffset, bounds.endOffset) ?: return
        val conversionContext = detectConversionContext(pasteTarget.pasteContext, text, project) ?: return
        if (!confirmConvertJavaOnPaste(project, isPlainText = true)) return

        val copiedJavaCode = prepareCopiedJavaCodeByContext(text, conversionContext, pasteTarget)
        val dataForConversion = DataForConversion.prepare(copiedJavaCode, project)

        fun convert() {
            val additionalImports = dataForConversion.tryResolveImports(targetFile)
            var convertedImportsText = additionalImports.convertCodeToKotlin(project, targetModule, useNewJ2k).text

            val convertedResult = dataForConversion.convertCodeToKotlin(project, targetModule, useNewJ2k)
            val convertedText = convertedResult.text

            val newBounds = runWriteAction {
                val importsInsertOffset = targetFile.importList?.endOffset ?: 0
                if (targetFile.importDirectives.isEmpty() && importsInsertOffset > 0)
                    convertedImportsText = "\n" + convertedImportsText

                if (convertedImportsText.isNotBlank())
                    editor.document.insertString(importsInsertOffset, convertedImportsText)

                val startOffset = bounds.startOffset
                editor.document.replaceString(startOffset, bounds.endOffset, convertedText)

                val endOffsetAfterCopy = startOffset + convertedText.length
                editor.caretModel.moveToOffset(endOffsetAfterCopy)

                editor.document.createRangeMarker(startOffset, startOffset + convertedText.length)
            }

            psiDocumentManager.commitAllDocuments()

            if (useNewJ2k) {
                val postProcessor = J2kConverterExtension.extension(useNewJ2k).createPostProcessor(formatCode = true)
                convertedResult.importsToAdd.forEach { fqName ->
                    postProcessor.insertImport(targetFile, fqName)
                }
            }

            runPostProcessing(project, targetFile, newBounds.range, convertedResult.converterContext, useNewJ2k)

            conversionPerformed = true
        }

        val conversionTime = measureTimeMillis { convert() }
        logJ2kConversionStatistics(
            ConversionType.TEXT_EXPRESSION,
            checkUseNewJ2k(targetFile),
            conversionTime,
            dataForConversion.elementsAndTexts.linesCount(),
            filesCount = 1
        )
    }

    private fun DataForConversion.convertCodeToKotlin(project: Project, targetModule: Module?, useNewJ2k: Boolean): ConversionResult {
        return elementsAndTexts.convertCodeToKotlin(project, targetModule, useNewJ2k)
    }

    private val KtElement.pasteContext: KotlinContext
        get() = when (this) {
            is KtFile -> KotlinContext.TOP_LEVEL
            is KtClassBody -> KotlinContext.CLASS_BODY
            is KtBlockExpression -> KotlinContext.IN_BLOCK
            else -> KotlinContext.EXPRESSION
        }

    private fun detectPasteTarget(file: KtFile, startOffset: Int, endOffset: Int): KtElement? {
        if (isNoConversionPosition(file, startOffset)) return null

        val fileText = file.text
        val dummyDeclarationText = "fun dummy(){}"
        val newFileText = "${fileText.substring(0, startOffset)} $dummyDeclarationText\n${fileText.substring(endOffset)}"

        val newFile = parseAsFile(newFileText, KotlinFileType.INSTANCE, file.project)
        (newFile as KtFile).analysisContext = file

        val funKeyword = newFile.findElementAt(startOffset + 1) ?: return null
        if (funKeyword.node.elementType != KtTokens.FUN_KEYWORD) return null
        val declaration = funKeyword.parent as? KtFunction ?: return null

        return declaration.parent as? KtElement
    }

    private fun detectConversionContext(pasteContext: KotlinContext, text: String, project: Project): JavaContext? {
        if (isParsedAsKotlinCode(text, pasteContext, project)) return null

        fun JavaContext.check(): JavaContext? =
            takeIf { isParsedAsJavaCode(text, it, project) }

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
        return PsiFileFactory.getInstance(project)
            .createFileFromText("Dummy." + fileType.defaultExtension, fileType, text, LocalTimeCounter.currentTime(), true)
    }

    private fun DataForConversion.tryResolveImports(targetFile: KtFile): ElementAndTextList {
        val importResolver = PlainTextPasteImportResolver(this, targetFile)
        importResolver.addImportsFromTargetFile()
        importResolver.tryResolveReferences()
        return ElementAndTextList(importResolver.addedImports.flatMap { importStatement ->
            listOf("\n", importStatement)
        } + "\n\n") //TODO Non-manual formatting for import list
    }

    private fun prepareCopiedJavaCodeByContext(text: String, context: JavaContext, target: KtElement): CopiedJavaCode {
        val targetFile = target.containingFile as KtFile
        val (localDeclarations, memberDeclarations) = javaContextDeclarationRenderer.render(target)

        val prefix = buildString {
            targetFile.packageDirective?.let {
                if (it.text.isNotEmpty()) {
                    append(it.text)
                    append(";\n")
                }
            }
        }

        val classDef = when (context) {
            JavaContext.TOP_LEVEL -> ""

            JavaContext.CLASS_BODY, JavaContext.IN_BLOCK, JavaContext.EXPRESSION -> {
                val lightClass = target.getParentOfType<KtClass>(false)?.toLightClass()

                buildString {
                    append("class ")
                    append(lightClass?.name ?: "Dummy")
                    lightClass?.extendsListTypes?.ifNotEmpty {
                        joinTo(
                            this@buildString,
                            prefix = " extends "
                        ) { it.getCanonicalText(true) }
                    }
                    lightClass?.implementsListTypes?.ifNotEmpty {
                        joinTo(this@buildString, prefix = " implements ") {
                            it.getCanonicalText(
                                true
                            )
                        }
                    }
                }
            }
        }

        return when (context) {
            JavaContext.TOP_LEVEL -> createCopiedJavaCode(prefix, "$", text)

            JavaContext.CLASS_BODY -> createCopiedJavaCode(prefix, "$classDef {\n$memberDeclarations $\n}", text)

            JavaContext.IN_BLOCK ->
                createCopiedJavaCode(prefix, "$classDef {\n$memberDeclarations void foo() {\n$localDeclarations $\n}\n}", text)

            JavaContext.EXPRESSION -> createCopiedJavaCode(prefix, "$classDef {\nObject field = $\n}", text)
        }
    }

    private fun createCopiedJavaCode(prefix: String, templateWithoutPrefix: String, text: String): CopiedJavaCode {
        val template = "$prefix$templateWithoutPrefix"
        val index = template.indexOf("$")
        assert(index >= 0)
        val fileText = template.substring(0, index) + text + template.substring(index + 1)
        return CopiedJavaCode(fileText, intArrayOf(index), intArrayOf(index + text.length))
    }

    companion object {
        @get:TestOnly
        var conversionPerformed: Boolean = false
    }
}
