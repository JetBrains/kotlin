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
import org.jetbrains.kotlin.analysis.test.framework.services.ExpressionMarkersSourceFilePreprocessor.TAGS.getCaretTagText
import org.jetbrains.kotlin.analysis.test.framework.services.ExpressionMarkersSourceFilePreprocessor.TAGS.getSelectionTagText
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.elementsInRange
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.SourceFilePreprocessor
import org.jetbrains.kotlin.test.services.TestService
import org.jetbrains.kotlin.test.services.TestServices
import java.util.*
import kotlin.reflect.KClass

internal class ExpressionMarkersSourceFilePreprocessor(testServices: TestServices) : SourceFilePreprocessor(testServices) {
    override fun process(file: TestFile, content: String): String {
        val processors = listOf(
            SourceFileProcessor(TAGS.SELECTION_REGEXP) { qualifier, range ->
                testServices.expressionMarkerProvider.addSelection(file, qualifier, TextRange(range.first, range.last + 1))
            },
            SourceFileProcessor(TAGS.CARET_REGEXP) { qualifier, range ->
                testServices.expressionMarkerProvider.addCaret(file, qualifier, range.first)
            }
        )

        return processText(content, processors)
    }

    private fun processText(text: String, processors: List<SourceFileProcessor>): String {
        var result = text

        nextMatch@ while (true) {
            // Find a processor with the most early matching tag
            val matches = sequence {
                for (processor in processors) {
                    val match = processor.regex.find(result) ?: continue
                    yield(processor to match)
                }
            }.sortedBy { it.second.range.first }

            val (processor, match) = matches.firstOrNull() ?: break
            val qualifier = match.groupValues[2]

            val startOffset = match.range.first
            val selectionGroup = if (match.groups.size >= 4) match.groups[3] else null

            val range = if (selectionGroup != null) {
                val delta = selectionGroup.range.first - startOffset
                IntRange(selectionGroup.range.first - delta, selectionGroup.range.last - delta)
            } else {
                IntRange(startOffset, startOffset)
            }

            processor.action(qualifier, range)

            val replacementText = selectionGroup?.value ?: ""
            result = result.replaceRange(match.range, replacementText)
        }

        return result
    }

    private class SourceFileProcessor(val regex: Regex, val action: (String, IntRange) -> Unit)

    object TAGS {
        val SELECTION_REGEXP = "<(expr(?:_(\\w+))?)>(.*?)</\\1>".toRegex(RegexOption.DOT_MATCHES_ALL)
        val CARET_REGEXP = "<(caret(?:_(\\w+))?)>".toRegex()

        fun getCaretTagText(qualifier: String): String = getTagText("caret", qualifier)
        fun getSelectionTagText(qualifier: String): String = getTagText("expr", qualifier)

        private fun getTagText(tagName: String, qualifier: String): String {
            return if (qualifier.isEmpty()) "<$tagName>" else "<${tagName}_$qualifier>"
        }
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

    /**
     * Returns the offset of a caret tag with the given [qualifier], or `null` if there is no such a tag.
     */
    fun getCaretOrNull(file: PsiFile, qualifier: String = ""): Int? {
        return carets.get(file.name, qualifier)
    }

    /**
     * Returns the offset of a caret tag with the given [qualifier]. Throws an exception if there is no such a tag.
     */
    @Throws(IllegalStateException::class)
    fun getCaret(file: PsiFile, qualifier: String = ""): Int {
        return getCaretOrNull(file, qualifier)
            ?: caretNotFoundError(getCaretTagText(qualifier))
    }

    /**
     * Returns all carets in the file.
     */
    fun getAllCarets(file: PsiFile): List<FileMarker<Int>> {
        return carets.getAll(file.name)
            .map { (qualifier, offset) -> FileMarker(qualifier, getCaretTagText(qualifier), offset) }
    }

    /**
     * Returns the text range enclosed in a selection tag with the given [qualifier], or `null` if there is no such a tag.
     */
    fun getSelectionOrNull(file: PsiFile, qualifier: String = ""): TextRange? {
        return selections.get(file.name, qualifier)
    }

    /**
     * Returns the text range enclosed in a selection tag with the given [qualifier]. Throws an exception if there is no such a tag.
     */
    @Throws(IllegalStateException::class)
    fun getSelection(file: PsiFile, qualifier: String = ""): TextRange {
        return getSelectionOrNull(file, qualifier)
            ?: caretNotFoundError(getSelectionTagText(qualifier))
    }

    /**
     * Returns all selections in the file.
     */
    fun getAllSelections(file: PsiFile): List<FileMarker<TextRange>> {
        return selections.getAll(file.name)
            .map { (qualifier, range) -> FileMarker(qualifier, getSelectionTagText(qualifier), range) }
    }

    /**
     * Returns the bottommost element of the type [T] at a caret tag with the given [qualifier].
     * Throws an exception if there is no such a tag or if the element under the tag has an incompatible type.
     */
    @Throws(NoSuchElementException::class)
    inline fun <reified T : PsiElement> getBottommostElementOfTypeAtCaret(file: PsiFile, qualifier: String = ""): T {
        return getBottommostElementOfTypeAtCaret(file, T::class, qualifier)
    }

    /**
     * Returns the bottommost element of the type [T] at a caret tag with the given [qualifier].
     * Throws an exception if there is no such a tag or if the element under the tag has an incompatible type.
     */
    @Throws(NoSuchElementException::class)
    fun <T : PsiElement> getBottommostElementOfTypeAtCaret(file: PsiFile, type: KClass<T>, qualifier: String = ""): T {
        return getBottommostElementOfTypeAtCaretOrNull(file, type, qualifier)
            ?: throw NoSuchElementException("Found no element on ${getCaretTagText(qualifier)} with the type ${type.simpleName}")
    }

    /**
     * Returns a bottommost element of the type [T] at a caret tag with the given [qualifier], or `null` if there is no such a tag
     * or if the element under the tag has an incompatible type.
     */
    inline fun <reified T : PsiElement> getBottommostElementOfTypeAtCaretOrNull(file: PsiFile, qualifier: String = ""): T? {
        return getBottommostElementOfTypeAtCaretOrNull(file, T::class, qualifier)
    }

    /**
     * Returns a bottommost element of the type [T] at a caret tag with the given [qualifier], or `null` if there is no such a tag
     * or if the element under the tag has an incompatible type.
     */
    fun <T : PsiElement> getBottommostElementOfTypeAtCaretOrNull(file: PsiFile, type: KClass<T>, qualifier: String = ""): T? {
        val offset = getCaretOrNull(file, qualifier) ?: return null
        val element = file.findElementAt(offset)
        return PsiTreeUtil.getParentOfType(element, type.java, false)
    }

    /**
     * Returns a list of bottommost elements of the type [T] at a caret tag with the given [qualifier] in each of the given [files].
     */
    inline fun <reified T : PsiElement> getBottommostElementsOfTypeAtCarets(
        files: List<PsiFile>,
        qualifier: String = "",
    ): List<Pair<T, PsiFile>> {
        return buildList {
            for (file in files) {
                val element = getBottommostElementOfTypeAtCaretOrNull<T>(file, qualifier) ?: continue
                add(element to file)
            }
        }
    }

    /**
     * Returns a list of bottommost elements of the type [T] at a caret with the given [qualifier] in every test file.
     */
    inline fun <reified T : PsiElement> getBottommostElementsOfTypeAtCarets(
        testServices: TestServices,
        qualifier: String = "",
    ): Collection<Pair<T, PsiFile>> {
        return testServices.ktTestModuleStructure.mainModules
            .flatMap { getBottommostElementsOfTypeAtCarets<T>(it.psiFiles, qualifier) }
    }

    /**
     * Returns a list of topmost elements enclosed in a selection tag with the given [qualifier] in the order as they appear
     * in the file.
     *
     * Such as, for `<expr>foo bar</expr>`, both `foo` and `bar` will be returned if there is no parent in the selection range
     * that includes both `foo` and `bar`.
     */
    private fun getTopmostSelectedElements(file: KtFile, qualifier: String = ""): List<PsiElement> {
        val range = getSelectionOrNull(file, qualifier) ?: return emptyList()

        val elements = if (range.isEmpty) {
            val candidates = file.collectDescendantsOfType<PsiElement> { it.textRange == range }
            buildList {
                for (candidate in candidates) {
                    // Search only for topmost descendants
                    if (candidates.none { it !== candidate && it.isAncestor(candidate, strict = true) }) {
                        add(candidate)
                    }
                }
            }
        } else {
            file.elementsInRange(range)
        }

        return elements.trimWhitespaces()
    }

    /**
     * Returns the topmost element enclosed in a selection tag with the given [qualifier].
     * Throws an exception if such an element does not exist or if there are multiple selected elements.
     */
    @Throws(IllegalStateException::class)
    fun getTopmostSelectedElement(file: KtFile, qualifier: String = ""): PsiElement {
        val elements = getTopmostSelectedElements(file, qualifier)
        return elements.singleOrNull() ?: singleElementError(elements)
    }

    /**
     * Returns the topmost element of the type [T] enclosed in a selection tag with the given [qualifier].
     * Throws an exception if there is no such a tag or if there is no element with the type [T].
     */
    @Throws(IllegalStateException::class)
    inline fun <reified T : PsiElement> getTopmostSelectedElementOfType(file: KtFile, qualifier: String = ""): T {
        return getTopmostSelectedElementOfType(file, T::class, qualifier)
    }

    /**
     * Returns the topmost element of the type [T] enclosed in a selection tag with the given [qualifier].
     * Throws an exception if there is no such a tag or if there is no element with the type [T].
     */
    @Throws(IllegalStateException::class)
    fun <T : PsiElement> getTopmostSelectedElementOfType(file: KtFile, type: KClass<T>, qualifier: String = ""): T {
        val elements = getTopmostSelectedElementsOfType(file, type, qualifier)
        return elements.singleOrNull() ?: singleElementError(elements)
    }

    /**
     * Returns the topmost element of the type [T] enclosed in a selection tag with the given [qualifier],
     * or `null` if there is no such element.
     */
    @Throws(IllegalStateException::class)
    private fun <T : PsiElement> getTopmostSelectedElementOfTypeOrNull(file: KtFile, type: KClass<T>, qualifier: String = ""): T? {
        val elements = getTopmostSelectedElementsOfType(file, type, qualifier)
        return elements.singleOrNull()
    }

    /**
     * Returns the topmost element of the type [T] enclosed in a selection tag with the given [qualifier].
     * Throws an exception if there is no such a tag or if there is no element with the type [T].
     */
    @Throws(IllegalStateException::class)
    private fun <T : PsiElement> getTopmostSelectedElementsOfType(file: KtFile, type: KClass<T>, qualifier: String = ""): List<T> {
        return getTopmostSelectedElements(file, qualifier).mapNotNull { getChildOfTypeOrNull(it, type) }
    }

    /**
     * Returns the [element] if it is of type [T], or its first descendant of the type [T] with the same text range,
     * or `null` if there are no such elements.
     */
    private fun <T : PsiElement> getChildOfTypeOrNull(element: PsiElement, type: KClass<T>): T? {
        if (type.isInstance(element)) {
            @Suppress("UNCHECKED_CAST")
            return element as T
        }

        val result = generateSequence(element) { it.children.singleOrNull() }
            .takeWhile { it.textRange == element.textRange }
            .firstOrNull { type.isInstance(it) }

        @Suppress("UNCHECKED_CAST")
        return result as T?
    }

    /**
     * Returns the bottommost element of the type [T] enclosed in an '<expr>' selection tag with the given [qualifier].
     * Throws an error if there are no such elements.
     */
    @Throws(NoSuchElementException::class)
    fun <T : PsiElement> getBottommostSelectedElementOfType(file: KtFile, type: KClass<T>, qualifier: String = ""): T {
        return getBottommostSelectedElementOfTypeOrNull(file, type, qualifier)
            ?: throw NoSuchElementException("Found no element of type ${type.simpleName} inside ${getSelectionTagText(qualifier)}")
    }

    /**
     * Returns the bottommost element of the type [T] enclosed in a selection tag with the given [qualifier],
     * or `null` if there is no selection tag with the given [qualifier].
     */
    private fun <T : PsiElement> getBottommostSelectedElementOfTypeOrNull(file: KtFile, type: KClass<T>, qualifier: String = ""): T? {
        val element = getTopmostSelectedElements(file, qualifier).singleOrNull() ?: return null

        val result = generateSequence(element) { it.children.singleOrNull() }
            .filter { type.isInstance(it) }
            .last { it.textRange == element.textRange }

        @Suppress("UNCHECKED_CAST")
        return result as T
    }

    /**
     * Returns the bottommost element of the type inferred from the [Directives.LOOK_UP_FOR_ELEMENT_OF_TYPE] directive.
     * If the directive is not found, the [defaultType] is used instead.
     * The method first tries to find the element enclosed in a selection tag and then looks up an element on a caret.
     * Throws an error if the element is not found.
     */
    fun getBottommostElementOfTypeByDirective(
        file: KtFile,
        module: TestModule,
        defaultType: KClass<out PsiElement> = PsiElement::class,
        qualifier: String = "",
    ): PsiElement {
        val type = findExpectedTypeClass(module.directives) ?: defaultType
        return getBottommostSelectedElementOfTypeOrNull(file, type, qualifier)
            ?: getBottommostElementOfTypeAtCaretOrNull(file, type, qualifier)
            ?: error("Neither <expr> marker nor <caret> were found in file")
    }

    /**
     * Returns the topmost element of the type inferred from the [Directives.LOOK_UP_FOR_ELEMENT_OF_TYPE] directive
     * enclosed in a selection tag with the given [qualifier].
     * Throws an error if the element is not found.
     */
    fun getTopmostSelectedElementOfTypeByDirective(
        file: KtFile,
        module: KtTestModule,
        defaultType: KClass<out PsiElement> = PsiElement::class,
        qualifier: String = "",
    ): PsiElement {
        val type = findExpectedTypeClass(module.testModule.directives) ?: defaultType
        return getTopmostSelectedElementOfType(file, type, qualifier)
    }

    /**
     * Returns the topmost element of the type inferred from the [Directives.LOOK_UP_FOR_ELEMENT_OF_TYPE] directive
     * enclosed in a selection tag with the given [qualifier] or `null` if the element is not found, or if there are multiple matching
     * elements.
     */
    fun getTopmostSelectedElementOfTypeByDirectiveOrNull(
        file: KtFile,
        module: KtTestModule,
        defaultType: KClass<out PsiElement> = PsiElement::class,
        qualifier: String = "",
    ): PsiElement? {
        val type = findExpectedTypeClass(module.testModule.directives) ?: defaultType
        return getTopmostSelectedElementOfTypeOrNull(file, type, qualifier)
    }

    /**
     * Returns the bottommost element of the type inferred from the [Directives.LOOK_UP_FOR_ELEMENT_OF_TYPE] directive
     * enclosed in a selection tag with the given [qualifier].
     * Throws an error if the element is not found.
     */
    fun getBottommostSelectedElementOfTypeByDirective(
        file: KtFile,
        module: KtTestModule,
        defaultType: KClass<out PsiElement> = PsiElement::class,
        qualifier: String = "",
    ): PsiElement {
        val type = findExpectedTypeClass(module.testModule.directives) ?: defaultType
        return getBottommostSelectedElementOfType(file, type, qualifier)
    }

    private fun findExpectedTypeClass(registeredDirectives: RegisteredDirectives): KClass<PsiElement>? {
        val expectedType = registeredDirectives.singleOrZeroValue(Directives.LOOK_UP_FOR_ELEMENT_OF_TYPE) ?: return null
        val ktPsiPackage = "org.jetbrains.kotlin.psi."
        val expectedTypeFqName = ktPsiPackage + expectedType.removePrefix(ktPsiPackage)

        @Suppress("UNCHECKED_CAST")
        return Class.forName(expectedTypeFqName).kotlin as KClass<PsiElement>
    }

    private fun List<PsiElement>.trimWhitespaces(): List<PsiElement> =
        dropWhile { it is PsiWhiteSpace }
            .dropLastWhile { it is PsiWhiteSpace }

    @Throws(IllegalStateException::class)
    private fun caretNotFoundError(tagText: String): Nothing {
        error("No '$tagText' tag was found in the file")
    }

    @Throws(IllegalStateException::class)
    private fun singleElementError(elements: Collection<PsiElement>): Nothing {
        val foundElements = elements.joinToString { it::class.simpleName + ": " + it.text }
        error("Expected a single element but found ${elements.size} [$foundElements]")
    }

    object Directives : SimpleDirectivesContainer() {
        val LOOK_UP_FOR_ELEMENT_OF_TYPE by stringDirective("LOOK_UP_FOR_ELEMENT_OF_TYPE")
    }
}

/**
 * @param qualifier a tag qualifier (e.g., 'foo' for the '<caret_foo>' tag).
 * @param tagText the complete opening tag text.
 * @param value the marker value (an offset or a text range).
 */
data class FileMarker<T : Any>(
    val qualifier: String,
    val tagText: String,
    val value: T,
)

fun FileMarker<TextRange>.toCaretMarker(): FileMarker<Int> {
    return FileMarker(qualifier, getCaretTagText(qualifier), value.startOffset)
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