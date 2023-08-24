/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.services

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analysis.test.framework.project.structure.ktModuleProvider
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.kotlin.util.PrivateForInline
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.elementsInRange
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.SourceFilePreprocessor
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestService
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import kotlin.reflect.KClass

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
    fun getCaretPositionOrNull(file: KtFile, caretTag: String? = null): Int? {
        return carets.getCaretOffset(file.name, caretTag)
    }

    fun getCaretPosition(file: KtFile, caretTag: String? = null): Int {
        return getCaretPositionOrNull(file, caretTag)
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

    inline fun <reified P : KtElement> getElementOfTypeAtCaretByDirective(
        file: KtFile,
        registeredDirectives: RegisteredDirectives,
        caretTag: String? = null,
    ): P {
        val elementAtCaret = getElementOfTypeAtCaret<P>(file, caretTag)
        val expectedType = expectedTypeClass(registeredDirectives) ?: return elementAtCaret
        return (elementAtCaret as PsiElement).parentsWithSelf.firstNotNullOfOrNull { currentElement ->
            currentElement.takeIf(expectedType::isInstance) as? P
        } ?: error("Element of ${P::class.simpleName} & ${expectedType.simpleName} is not found")
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

    fun getSelectedElementOrElementAtCaretOfTypeByDirective(
        ktFile: KtFile,
        module: TestModule,
        defaultType: KClass<out PsiElement>? = null,
        caretTag: String? = null,
    ): PsiElement {
        val expectedClass = expectedTypeClass(module.directives) ?: defaultType?.java
        return getSelectedElementOfClassOrNull(ktFile, expectedClass)
            ?: getElementOfClassAtCaretOrNull(ktFile, expectedClass, caretTag)
            ?: error("Neither ${ExpressionMarkersSourceFilePreprocessor.TAGS.OPENING_EXPRESSION_TAG} marker nor <caret> were found in file")
    }

    private fun getSelectedElementOfClassOrNull(
        ktFile: KtFile,
        expectedClass: Class<out PsiElement>?,
    ): PsiElement? {
        val selectedElement = getSelectedElementOrNull(ktFile) ?: return null
        if (expectedClass == null) return selectedElement
        return findDescendantOfTheSameRangeOfType(selectedElement, expectedClass)
    }


    private fun getElementOfClassAtCaretOrNull(
        ktFile: KtFile,
        expectedClass: Class<out PsiElement>?,
        caretTag: String? = null,
    ): PsiElement? {
        val caretPosition = getCaretPositionOrNull(ktFile, caretTag) ?: return null
        val elementAtPosition = ktFile.findElementAt(caretPosition) ?: return null
        if (expectedClass == null) return elementAtPosition
        return PsiTreeUtil.getParentOfType(elementAtPosition, expectedClass, /*strict*/false)
    }


    fun getSelectedElementOrNull(file: KtFile): PsiElement? {
        val range = selected[file.name] ?: return null
        val elements = file.elementsInRange(range).trimWhitespaces()
        if (elements.size != 1) {
            error("Expected one element at rage but found ${elements.size} [${elements.joinToString { it::class.simpleName + ": " + it.text }}]")
        }
        return elements.single()
    }

    fun getSelectedElement(file: KtFile): PsiElement {
        return getSelectedElementOrNull(file)
            ?: error("No selected expression found in file")
    }

    fun expectedTypeClass(registeredDirectives: RegisteredDirectives): Class<PsiElement>? {
        val expectedType = registeredDirectives.singleOrZeroValue(Directives.LOOK_UP_FOR_ELEMENT_OF_TYPE) ?: return null
        val ktPsiPackage = "org.jetbrains.kotlin.psi."
        val expectedTypeFqName = ktPsiPackage + expectedType.removePrefix(ktPsiPackage)

        @Suppress("UNCHECKED_CAST")
        return Class.forName(expectedTypeFqName) as Class<PsiElement>
    }

    fun getSelectedElementOfTypeByDirective(ktFile: KtFile, module: TestModule): PsiElement {
        val selectedElement = getSelectedElement(ktFile)
        val expectedType = expectedTypeClass(module.directives) ?: return selectedElement
        if (expectedType.isInstance(selectedElement)) return selectedElement

        return findDescendantOfTheSameRangeOfType(selectedElement, expectedType)
    }

    private fun findDescendantOfTheSameRangeOfType(selectedElement: PsiElement, expectedClass: Class<out PsiElement>): PsiElement {
        return selectedElement.collectDescendantsOfType<PsiElement> {
            expectedClass.isInstance(it)
        }.single { it.textRange == selectedElement.textRange }
    }

    inline fun <reified E : KtElement> getSelectedElementOfType(file: KtFile): E {
        return when (val selected = getSelectedElement(file)) {
            is E -> selected
            else -> generateSequence(selected) { current ->
                current.children.singleOrNull()?.takeIf { it.textRange == current.textRange }
            }.firstIsInstance()
        }
    }

    /**
     * Find the bottommost element of an [elementType] or its subtype located precisely in the [range].
     */
    private fun <T : PsiElement> getBottommostElementOfTypeInRange(file: KtFile, range: TextRange, elementType: Class<T>): T {
        var candidate = PsiTreeUtil.findElementOfClassAtOffset(file, range.startOffset, elementType, true)
        while (candidate != null && candidate.endOffset < range.endOffset) {
            candidate = PsiTreeUtil.getParentOfType(candidate, elementType)?.takeIf { it.startOffset == range.startOffset }
        }

        return candidate?.takeIf { it.endOffset == range.endOffset }
            ?: error("Cannot find '${elementType.name}' in range $range")
    }

    /**
     * Find the bottommost element of [E] or its subtype wrapped in an '<expr>' selection tag.
     */
    fun <E : KtElement> getBottommostSelectedElementOfType(file: KtFile, elementType: Class<E>): E {
        val range = selected[file.name] ?: error("No selected expression found in file")
        return getBottommostElementOfTypeInRange(file, range, elementType)
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
