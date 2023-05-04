/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.services

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.analysis.test.framework.project.structure.ktModuleProvider
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.kotlin.fir.PrivateForInline
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.elementsInRange
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.SourceFilePreprocessor
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestService
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance

internal class ExpressionMarkersSourceFilePreprocessor(testServices: TestServices) : SourceFilePreprocessor(testServices) {
    override fun process(file: TestFile, content: String): String {
        val withSelectedProcessed = processSelectedExpression(file, content)
        return processCaretExpression(file, withSelectedProcessed)
    }

    private fun processSelectedExpression(file: TestFile, content: String): String {
        val startCaretPosition = content.indexOfOrNull(TAGS.OPENING_EXPRESSION_TAG) ?: return content

        val endCaretPosition = content.indexOfOrNull(TAGS.CLOSING_EXPRESSION_TAG)
            ?: error("${TAGS.CLOSING_EXPRESSION_TAG} was not found in the file")

        check(startCaretPosition < endCaretPosition)
        testServices.expressionMarkerProvider.addSelectedExpression(
            file,
            TextRange.create(startCaretPosition, endCaretPosition - TAGS.OPENING_EXPRESSION_TAG.length)
        )
        return content
            .replace(TAGS.OPENING_EXPRESSION_TAG, "")
            .replace(TAGS.CLOSING_EXPRESSION_TAG, "")
    }

    private fun processCaretExpression(file: TestFile, content: String): String {
        var result = content
        var match = TAGS.CARET_REGEXP.find(result)
        while (match != null) {
            val startCaretPosition = match.range.first
            val tag = match.groups[2]?.value
            testServices.expressionMarkerProvider.addCaret(file, tag, startCaretPosition)
            result = result.removeRange(match.range)
            match = TAGS.CARET_REGEXP.find(result)
        }
        return result
    }

    object TAGS {
        const val OPENING_EXPRESSION_TAG = "<expr>"
        const val CLOSING_EXPRESSION_TAG = "</expr>"
        val CARET_REGEXP = "<caret(_(\\w+))?>".toRegex()
    }
}

class ExpressionMarkerProvider : TestService {
    private val selected = mutableMapOf<String, TextRange>()

    @PrivateForInline
    val carets = CaretProvider()

    fun addSelectedExpression(file: TestFile, range: TextRange) {
        selected[file.relativePath] = range
    }

    @OptIn(PrivateForInline::class)
    fun addCaret(file: TestFile, caretTag: String?, caretOffset: Int) {
        carets.addCaret(file.name, caretTag, caretOffset)
    }

    @OptIn(PrivateForInline::class)
    fun getCaretPosition(file: KtFile, caretTag: String? = null): Int {
        return carets.getCaretOffset(file.name, caretTag)
            ?: run {
                val caretName = "caret${caretTag?.let { "_$it" }.orEmpty()}"
                error("No <$caretName> found in file")
            }

    }

    inline fun <reified P : KtElement> getElementOfTypeAtCaret(file: KtFile, caretTag: String? = null): P {
        val offset = getCaretPosition(file, caretTag)
        return file.findElementAt(offset)
            ?.parentOfType()
            ?: error("No expression found at caret")
    }

    @OptIn(PrivateForInline::class)
    inline fun <reified P : KtElement> getElementsOfTypeAtCarets(
        files: Collection<KtFile>,
        caretTag: String? = null
    ): Collection<Pair<P, KtFile>> {
        return files.mapNotNull { file ->
            carets.getCaretOffset(file.name, caretTag)?.let { offset ->
                file.findElementAt(offset)?.parentOfType<P>()?.let { element ->
                    element to file
                }
            }
        }
    }

    inline fun <reified P : KtElement> getElementsOfTypeAtCarets(
        moduleStructure: TestModuleStructure,
        testServices: TestServices,
        caretTag: String? = null
    ): Collection<Pair<P, KtFile>> {
        return moduleStructure.modules.flatMap { module ->
            val ktFiles = testServices.ktModuleProvider.getModuleFiles(module).filterIsInstance<KtFile>()
            getElementsOfTypeAtCarets<P>(ktFiles, caretTag)
        }

    }

    fun getSelectedElement(file: KtFile): KtElement {
        val range = selected[file.name]
            ?: error("No selected expression found in file")
        val elements = file.elementsInRange(range).trimWhitespaces()
        if (elements.size != 1) {
            error("Expected one element at rage but found ${elements.size} [${elements.joinToString { it::class.simpleName + ": " + it.text }}]")
        }
        return elements.single() as KtElement
    }

    fun getSelectedElementOfTypeByDirective(ktFile: KtFile, module: TestModule): PsiElement {
        val selectedElement = getSelectedElement(ktFile)
        val expectedType = module.directives[Directives.LOOK_UP_FOR_ELEMENT_OF_TYPE].firstOrNull() ?: return selectedElement
        val ktPsiPackage = "org.jetbrains.kotlin.psi."
        val expectedTypeFqName = ktPsiPackage + expectedType.removePrefix(ktPsiPackage)
        @Suppress("UNCHECKED_CAST") val expectedClass = Class.forName(expectedTypeFqName) as Class<PsiElement>
        if (expectedClass.isInstance(selectedElement)) return selectedElement

        return selectedElement.collectDescendantsOfType<PsiElement> {
            expectedClass.isInstance(it)
        }.single { it.textRange == selectedElement.textRange }
    }

    inline fun <reified E : KtElement> getSelectedElementOfType(file: KtFile): E {
        return when (val selected = getSelectedElement(file)) {
            is E -> selected
            else -> generateSequence(selected as PsiElement) { current ->
                current.children.singleOrNull()?.takeIf { it.textRange == current.textRange }
            }.firstIsInstance()
        }
    }

    private fun List<PsiElement>.trimWhitespaces(): List<PsiElement> =
        dropWhile { it is PsiWhiteSpace }
            .dropLastWhile { it is PsiWhiteSpace }

    object Directives : SimpleDirectivesContainer() {
        val LOOK_UP_FOR_ELEMENT_OF_TYPE by stringDirective("LOOK_UP_FOR_ELEMENT_OF_TYPE")
    }
}

@PrivateForInline
class CaretProvider {
    private val caretToFile = mutableMapOf<String, CaretsInFile>()

    fun getCaretOffset(filename: String, caretTag: String?): Int? {
        val cartsInFile = caretToFile[filename] ?: return null
        return cartsInFile.getCaretOffsetByTag(caretTag)
    }

    fun addCaret(filename: String, caretTag: String?, caretOffset: Int) {
        val cartsInFile = caretToFile.getOrPut(filename) { CaretsInFile() }
        cartsInFile.addCaret(caretTag, caretOffset)
    }

    private class CaretsInFile {
        private val carets = mutableMapOf<String, Int>()

        fun getCaretOffsetByTag(tag: String?): Int? {
            return carets[tag.orEmpty()]
        }

        fun addCaret(caretTag: String?, caretOffset: Int) {
            carets[caretTag.orEmpty()] = caretOffset
        }
    }
}

val TestServices.expressionMarkerProvider: ExpressionMarkerProvider by TestServices.testServiceAccessor()

fun String.indexOfOrNull(substring: String) =
    indexOf(substring).takeIf { it >= 0 }