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

import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.elementsInRange
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.psi.psiUtil.siblings
import java.util.*

data class DataForConversion private constructor(
        val elementsAndTexts: ElementAndTextList /* list consisting of PsiElement's to convert and plain String's */,
        val importsAndPackage: String,
        val file: PsiJavaFile
) {
    companion object {
        fun prepare(copiedCode: CopiedJavaCode, project: Project): DataForConversion  {
            val startOffsets = copiedCode.startOffsets.clone()
            val endOffsets = copiedCode.endOffsets.clone()
            assert(startOffsets.size == endOffsets.size) { "Must have the same size" }

            var fileText = copiedCode.fileText
            var file = PsiFileFactory.getInstance(project).createFileFromText(JavaLanguage.INSTANCE, fileText) as PsiJavaFile

            val importsAndPackage = buildImportsAndPackage(file)

            val newFileText = clipTextIfNeeded(file, fileText, startOffsets, endOffsets)
            if (newFileText != null) {
                fileText = newFileText
                file = PsiFileFactory.getInstance(project).createFileFromText(JavaLanguage.INSTANCE, newFileText) as PsiJavaFile
            }

            val elementsAndTexts = ElementAndTextList()
            for (i in startOffsets.indices) {
                elementsAndTexts.collectElementsToConvert(file, fileText, TextRange(startOffsets[i], endOffsets[i]))
            }

            return DataForConversion(elementsAndTexts, importsAndPackage, file)
        }

        private fun clipTextIfNeeded(file: PsiJavaFile, fileText: String, startOffsets: IntArray, endOffsets: IntArray): String? {
            val ranges = startOffsets.indices.map { TextRange(startOffsets[it], endOffsets[it]) }.sortedBy { it.start }

            fun canDropRange(range: TextRange) = ranges.all { range !in it }

            val rangesToDrop = ArrayList<TextRange>()
            for (range in ranges) {
                val start = range.start
                val end = range.end
                if (start == end) continue

                val startToken = file.findElementAt(start)!!
                val elementToClipLeft = startToken.maximalParentToClip(range)
                if (elementToClipLeft != null) {
                    val elementStart = elementToClipLeft.range.start
                    if (elementStart < start) {
                        val clipBound = tryClipLeftSide(elementToClipLeft, start)
                        if (clipBound != null) {
                            val rangeToDrop = TextRange(elementStart, clipBound)
                            if (canDropRange(rangeToDrop)) {
                                rangesToDrop.add(rangeToDrop)
                            }
                        }
                    }
                }

                val endToken = file.findElementAt(end - 1)!!
                val elementToClipRight = endToken.maximalParentToClip(range)
                if (elementToClipRight != null) {
                    val elementEnd = elementToClipRight.range.end
                    if (elementEnd > end) {
                        val clipBound = tryClipRightSide(elementToClipRight, end)
                        if (clipBound != null) {
                            val rangeToDrop = TextRange(clipBound, elementEnd)
                            if (canDropRange(rangeToDrop)) {
                                rangesToDrop.add(rangeToDrop)
                            }
                        }
                    }
                }
            }

            if (rangesToDrop.isEmpty()) return null

            val newFileText = buildString {
                var offset = 0
                for (range in rangesToDrop) {
                    assert(range.start >= offset)
                    append(fileText.substring(offset, range.start))
                    offset = range.end
                }
                append(fileText.substring(offset, fileText.length))
            }

            fun IntArray.update() {
                for (range in rangesToDrop.asReversed()) {
                    for (i in indices) {
                        val offset = this[i]
                        if (offset >= range.end) {
                            this[i] = offset - range.length
                        }
                        else {
                            assert(offset <= range.start)
                        }
                    }
                }
            }

            startOffsets.update()
            endOffsets.update()

            return newFileText
        }

        private fun PsiElement.maximalParentToClip(range: TextRange): PsiElement? {
            val firstNotInRange = parentsWithSelf.takeWhile { it !is PsiDirectory }.firstOrNull { it.range !in range } ?: return null
            return firstNotInRange.parentsWithSelf.lastOrNull { it.minimizedTextRange() in range }
        }

        private fun PsiElement.minimizedTextRange(): TextRange {
            val firstChild = firstChild?.siblings()?.firstOrNull { !canDropElementFromText(it) } ?: return range
            val lastChild = lastChild.siblings(forward = false).first { !canDropElementFromText(it) }
            return TextRange(firstChild.minimizedTextRange().start, lastChild.minimizedTextRange().end)
        }

        // element's text can be removed from file's text keeping parsing the same
        private fun canDropElementFromText(element: PsiElement): Boolean {
            return when (element) {
                is PsiWhiteSpace, is PsiComment, is PsiModifierList, is PsiAnnotation -> true

                is PsiJavaToken -> {
                    when (element.tokenType) {
                        // modifiers
                        JavaTokenType.PUBLIC_KEYWORD, JavaTokenType.PROTECTED_KEYWORD, JavaTokenType.PRIVATE_KEYWORD,
                        JavaTokenType.STATIC_KEYWORD, JavaTokenType.ABSTRACT_KEYWORD, JavaTokenType.FINAL_KEYWORD,
                        JavaTokenType.NATIVE_KEYWORD, JavaTokenType.SYNCHRONIZED_KEYWORD, JavaTokenType.STRICTFP_KEYWORD,
                        JavaTokenType.TRANSIENT_KEYWORD, JavaTokenType.VOLATILE_KEYWORD, JavaTokenType.DEFAULT_KEYWORD -> element.getParent() is PsiModifierList

                        JavaTokenType.SEMICOLON -> true

                        else -> false
                    }
                }

                is PsiCodeBlock -> element.getParent() is PsiMethod

                else -> element.firstChild == null
            }
        }

        private fun tryClipLeftSide(element: PsiElement, leftBound: Int)
                = tryClipSide(element, leftBound, { range }, { allChildren })

        private fun tryClipRightSide(element: PsiElement, rightBound: Int): Int? {
            fun Int.transform() = Int.MAX_VALUE - this
            fun TextRange.transform() = TextRange(end.transform(), start.transform())
            return tryClipSide(element, rightBound.transform(), { range.transform() }, { lastChild.siblings(forward = false) })?.transform()
        }

        private fun tryClipSide(
                element: PsiElement,
                rangeBound: Int,
                rangeFunction: PsiElement.() -> TextRange,
                childrenFunction: PsiElement.() -> Sequence<PsiElement>
        ): Int? {
            if (element.firstChild == null) return null

            val elementRange = element.rangeFunction()
            assert(elementRange.start < rangeBound && rangeBound < elementRange.end)

            var clipTo = elementRange.start
            for (child in element.childrenFunction()) {
                val childRange = child.rangeFunction()

                if (childRange.start >= rangeBound) { // we have cut enough already
                    break
                }
                else if (childRange.end <= rangeBound) { // need to drop the whole element
                    if (!canDropElementFromText(child)) return null
                    clipTo = childRange.end
                }
                else { // rangeBound is inside child's range
                    if (child is PsiWhiteSpace) break // no need to cut whitespace - we can leave it as is
                    return tryClipSide(child, rangeBound, rangeFunction, childrenFunction)
                }
            }

            return clipTo
        }

        private fun ElementAndTextList.collectElementsToConvert(
                file: PsiJavaFile,
                fileText: String,
                range: TextRange
        ) {
            val elements = file.elementsInRange(range)
            if (elements.isEmpty()) {
                add(fileText.substring(range.start, range.end))
            }
            else {
                add(fileText.substring(range.start, elements.first().range.start))
                elements.forEach {
                    if (shouldExpandToChildren(it))
                        this += it.allChildren.toList()
                    else
                        this += it
                }
                add(fileText.substring(elements.last().range.end, range.end))
            }
        }

        // converting of PsiModifierList is not supported by converter, but converting of single annotations inside it is supported
        private fun shouldExpandToChildren(element: PsiElement) = element is PsiModifierList

        private fun buildImportsAndPackage(sourceFile: PsiJavaFile): String {
            return buildString {
                val packageName = sourceFile.packageName
                if (!packageName.isEmpty()) {
                    append("package $packageName\n")
                }

                val importList = sourceFile.importList
                if (importList != null) {
                    for (import in importList.importStatements) {
                        val qualifiedName = import.qualifiedName ?: continue
                        if (import.isOnDemand) {
                            append("import $qualifiedName.*\n")
                        }
                        else {
                            val fqName = FqNameUnsafe(qualifiedName)
                            // skip explicit imports of platform classes mapped into Kotlin classes
                            if (fqName.isSafe && JavaToKotlinClassMap.isJavaPlatformClass(fqName.toSafe())) continue
                            append("import $qualifiedName\n")
                        }
                    }
                    //TODO: static imports
                }
            }
        }
    }
}
