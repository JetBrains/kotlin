/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.services

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.KtTestModule
import org.jetbrains.kotlin.analysis.test.framework.projectStructure.ktTestModuleStructure
import org.jetbrains.kotlin.analysis.utils.printer.parentOfType
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfTypeTo
import org.jetbrains.kotlin.psi.psiUtil.elementsInRange
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.SourceFilePreprocessor
import org.jetbrains.kotlin.test.services.TestService
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.util.PrivateForInline
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import java.util.Collections
import kotlin.reflect.KClass

internal class ExpressionMarkersSourceFilePreprocessor(testServices: TestServices) : SourceFilePreprocessor(testServices) {
    override fun process(file: TestFile, content: String): String {
        return processCaretMarkers(file, processSelectedMarkers(file, content))
    }

    private fun processSelectedMarkers(file: TestFile, content: String): String {
        return processText(content, TAGS.SELECTION_REGEXP) { qualifier, range ->
            testServices.expressionMarkerProvider.addSelection(file, qualifier, TextRange(range.first, range.last + 1))
        }
    }

    private fun processCaretMarkers(file: TestFile, content: String): String {
        return processText(content, TAGS.CARET_REGEXP) { qualifier, range ->
            testServices.expressionMarkerProvider.addCaret(file, qualifier, range.first)
        }
    }

    private fun processText(text: String, regex: Regex, action: (String, IntRange) -> Unit): String {
        var result = text

        while (true) {
            val match = regex.find(result) ?: break
            val qualifier = match.groupValues[2]

            val startOffset = match.range.first
            val selectionGroup = match.groups[3]

            val range = if (selectionGroup != null) {
                val delta = selectionGroup.range.first - startOffset
                IntRange(selectionGroup.range.first - delta, selectionGroup.range.last - delta)
            } else {
                IntRange(startOffset, startOffset)
            }

            action(qualifier, range)

            val replacementText = match.groupValues.getOrNull(3) ?: ""
            result = result.replaceRange(match.range, replacementText)
        }

        return result
    }

    object TAGS {
        val SELECTION_REGEXP = "<(expr(?:_(\\w+))?)>(.+?)</\\1>".toRegex()
        val CARET_REGEXP = "<(caret(?:_(\\w+))?)>".toRegex()
    }
}

class ExpressionMarkerProvider : TestService {
    private val selections = FileMarkerStorage<String, TextRange>()
    private val carets = FileMarkerStorage<String, Int>()

    fun addSelection(file: TestFile, qualifier: String, range: TextRange) {
        selections.add(file.name, qualifier, range)
    }

    fun addCaret(file: TestFile, qualifier: String, caretOffset: Int) {
        carets.add(file.name, qualifier, caretOffset)
    }

    fun getCaretPositionOrNull(file: PsiFile, caretTag: String? = null): Int? {
        return carets.get(file.name, caretTag.orEmpty())
    }

    fun getCaretPosition(file: PsiFile, caretTag: String? = null): Int {
        return getCaretPositionOrNull(file, caretTag)
            ?: run {
                val caretName = "caret${caretTag?.let { "_$it" }.orEmpty()}"
                error("No <$caretName> found in file")
            }
    }

    fun getAllCarets(file: PsiFile): List<CaretMarker> {
        return carets.getAll(file.name)
            .map { (qualifier, offset) -> CaretMarker(qualifier, offset) }
    }

    fun getSelectedRangeOrNull(file: PsiFile): TextRange? = selections.get(file.name, qualifier = "")
    fun getSelectedRange(file: PsiFile): TextRange = getSelectedRangeOrNull(file) ?: error("No selected expression found in file")

    inline fun <reified P : PsiElement> getElementOfTypeAtCaret(file: PsiFile, caretTag: String? = null): P {
        val offset = getCaretPosition(file, caretTag)
        return file.findElementAt(offset)?.parentOfType() ?: error("No expression found at caret")
    }

    /**
     * Returns an element of type [P] at the specified caret, or returns `null` if no such caret exists. If the caret can be found but the
     * element has the wrong type, an error will be raised.
     */
    inline fun <reified P : KtElement> getElementOfTypeAtCaretOrNull(file: KtFile, caretTag: String? = null): P? {
        val offset = getCaretPositionOrNull(file, caretTag) ?: return null
        return file.findElementAt(offset)
            ?.parentOfType()
            ?: error("Element at caret doesn't exist or doesn't have a parent of type `${P::class.simpleName}`")
    }

    @OptIn(PrivateForInline::class)
    inline fun <reified P : PsiElement> getElementsOfTypeAtCarets(
        files: Collection<PsiFile>,
        caretTag: String? = null,
    ): Collection<Pair<P, PsiFile>> {
        return files.mapNotNull { file ->
            getCaretPositionOrNull(file, caretTag)?.let { offset ->
                file.findElementAt(offset)?.parentOfType<P>()?.let { element ->
                    element to file
                }
            }
        }
    }

    inline fun <reified P : PsiElement> getElementsOfTypeAtCarets(
        testServices: TestServices,
        caretTag: String? = null,
    ): Collection<Pair<P, PsiFile>> {
        return testServices.ktTestModuleStructure.mainModules.flatMap { ktTestModule ->
            getElementsOfTypeAtCarets<P>(ktTestModule.files, caretTag)
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
            ?: error("Neither <expr> marker nor <caret> were found in file")
    }

    private fun getSelectedElementOfClassOrNull(
        ktFile: KtFile,
        expectedClass: Class<out PsiElement>?,
    ): PsiElement? {
        if (expectedClass == null) return getSelectedElementOrNull(ktFile)

        val selectedElements = getSelectedElementsOrNull(ktFile) ?: return null
        return findDescendantOfTheSameRangeOfType(selectedElements, expectedClass)
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
        val elements = getSelectedElementsOrNull(file) ?: return null
        if (elements.size != 1) {
            singleElementError(elements)
        }

        return elements.single()
    }

    private fun singleElementError(elements: Collection<PsiElement>): Nothing {
        error("Expected one element at range but found ${elements.size} [${elements.joinToString { it::class.simpleName + ": " + it.text }}]")
    }

    fun getSelectedElementsOrNull(file: KtFile): List<PsiElement>? {
        val range = getSelectedRangeOrNull(file) ?: return null
        val elements = if (range.isEmpty) {
            file.collectDescendantsOfType<PsiElement> { it.textRange == range }
        } else {
            file.elementsInRange(range)
        }.trimWhitespaces()

        return elements
    }

    fun getSelectedElements(file: KtFile): List<PsiElement> {
        val range = getSelectedRange(file)
        val elements = if (range.isEmpty) {
            file.collectDescendantsOfType<PsiElement> { it.textRange == range }
        } else {
            file.elementsInRange(range)
        }.trimWhitespaces()

        return elements.ifEmpty { error("No selected expression found") }
    }

    fun getSelectedElement(file: KtFile): PsiElement {
        val selectedElements = getSelectedElements(file)
        return selectedElements.singleOrNull() ?: singleElementError(selectedElements)
    }

    fun expectedTypeClass(registeredDirectives: RegisteredDirectives): Class<PsiElement>? {
        val expectedType = registeredDirectives.singleOrZeroValue(Directives.LOOK_UP_FOR_ELEMENT_OF_TYPE) ?: return null
        val ktPsiPackage = "org.jetbrains.kotlin.psi."
        val expectedTypeFqName = ktPsiPackage + expectedType.removePrefix(ktPsiPackage)

        @Suppress("UNCHECKED_CAST")
        return Class.forName(expectedTypeFqName) as Class<PsiElement>
    }

    fun getSelectedElementOfTypeByDirective(
        ktFile: KtFile,
        module: KtTestModule,
        defaultType: KClass<out PsiElement>? = null,
    ): PsiElement {
        val expectedType = expectedTypeClass(module.testModule.directives) ?: defaultType?.java ?: return getSelectedElement(ktFile)

        val selectedElements = getSelectedElements(ktFile)
        selectedElements.filter(expectedType::isInstance).ifNotEmpty {
            return singleOrNull() ?: singleElementError(this)
        }

        return findDescendantOfTheSameRangeOfType(selectedElements, expectedType)
    }

    private fun findDescendantOfTheSameRangeOfType(selectedElements: List<PsiElement>, expectedClass: Class<out PsiElement>): PsiElement {
        val result = mutableSetOf<PsiElement>()
        for (element in selectedElements) {
            element.collectDescendantsOfTypeTo(result, { true }) {
                expectedClass.isInstance(it) && it.textRange == element.textRange
            }
        }

        return result.singleOrNull() ?: singleElementError(result)
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
        val range = getSelectedRange(file)
        return getBottommostElementOfTypeInRange(file, range, elementType)
    }

    private fun List<PsiElement>.trimWhitespaces(): List<PsiElement> =
        dropWhile { it is PsiWhiteSpace }
            .dropLastWhile { it is PsiWhiteSpace }

    object Directives : SimpleDirectivesContainer() {
        val LOOK_UP_FOR_ELEMENT_OF_TYPE by stringDirective("LOOK_UP_FOR_ELEMENT_OF_TYPE")
    }
}

data class CaretMarker(val tag: String, val offset: Int) {

    /**
     * Full render of this caret marker in the `<caret>` or `<caret_$tag>` form.
     *
     * @see ExpressionMarkersSourceFilePreprocessor.TAGS.CARET_REGEXP
     */
    val fullTag: String
        get() = if (tag.isEmpty()) {
            "<caret>"
        } else {
            "<caret_${tag}>"
        }
}

private class FileMarkerStorage<K : Any, T : Any> {
    private val markersByFile = mutableMapOf<K, FileMarkers<T>>()

    fun get(key: K, qualifier: String): T? {
        val fileMarkers = markersByFile[key] ?: return null
        return fileMarkers.get(qualifier)
    }

    fun getAll(key: K): Map<String, T> {
        return markersByFile[key]?.getAll().orEmpty()
    }

    fun add(key: K, qualifier: String, value: T) {
        val fileMarkers = markersByFile.getOrPut(key) { FileMarkers() }
        fileMarkers.add(qualifier, value)
    }

    private class FileMarkers<T : Any> {
        private val markersByTag = mutableMapOf<String, T>()

        fun get(qualifier: String): T? {
            return markersByTag[qualifier]
        }

        fun getAll(): Map<String, T> {
            return Collections.unmodifiableMap(markersByTag)
        }

        fun add(qualifier: String, value: T) {
            markersByTag[qualifier] = value
        }
    }
}

val TestServices.expressionMarkerProvider: ExpressionMarkerProvider by TestServices.testServiceAccessor()