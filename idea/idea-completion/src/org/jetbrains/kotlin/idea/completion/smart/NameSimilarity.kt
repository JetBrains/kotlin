/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.smart

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementWeigher
import com.intellij.codeInsight.lookup.WeighingContext
import com.intellij.openapi.util.Key
import com.intellij.psi.codeStyle.NameUtil
import org.jetbrains.kotlin.idea.core.ExpectedInfo
import kotlin.math.min

val NAME_SIMILARITY_KEY = Key<Int>("NAME_SIMILARITY_KEY")

object NameSimilarityWeigher : LookupElementWeigher("kotlin.nameSimilarity") {
    override fun weigh(element: LookupElement, context: WeighingContext) = -(element.getUserData(NAME_SIMILARITY_KEY) ?: 0)
}

fun calcNameSimilarity(name: String, expectedInfos: Collection<ExpectedInfo>): Int =
    expectedInfos.mapNotNull { it.expectedName }.maxOfOrNull { calcNameSimilarity(name, it) } ?: 0

private fun calcNameSimilarity(name: String, expectedName: String): Int {
    val words1 = NameUtil.nameToWordsLowerCase(name)
    val words2 = NameUtil.nameToWordsLowerCase(expectedName)

    val matchedWords = words1.toSet().intersect(words2)
    if (matchedWords.isEmpty()) return 0

    fun isNonNumber(word: String) = !word[0].isDigit()
    val nonNumberWords1 = words1.filter(::isNonNumber)
    val nonNumberWords2 = words2.filter(::isNonNumber)

    // count number of words matched at the end (but ignore number words - they are less important)
    val minWords = min(nonNumberWords1.size, nonNumberWords2.size)
    val matchedTailLength = (0 until minWords).firstOrNull { i ->
        nonNumberWords1[nonNumberWords1.size - i - 1] != nonNumberWords2[nonNumberWords2.size - i - 1]
    } ?: minWords

    return matchedWords.size * 1000 + matchedTailLength
}
