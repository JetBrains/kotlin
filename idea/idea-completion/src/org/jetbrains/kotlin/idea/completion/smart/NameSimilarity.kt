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

package org.jetbrains.kotlin.idea.completion.smart

import com.intellij.openapi.util.Key
import com.intellij.codeInsight.lookup.LookupElementWeigher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.WeighingContext
import org.jetbrains.kotlin.idea.completion.ExpectedInfo
import com.intellij.psi.codeStyle.NameUtil

val NAME_SIMILARITY_KEY = Key<Int>("NAME_SIMILARITY_KEY")

object NameSimilarityWeigher : LookupElementWeigher("kotlin.nameSimilarity") {
    override fun weigh(element: LookupElement, context: WeighingContext)
            = -(element.getUserData(NAME_SIMILARITY_KEY) ?: 0)
}

fun calcNameSimilarity(name: String, expectedInfos: Collection<ExpectedInfo>): Int {
    return expectedInfos
            .map { it.expectedName }
            .filterNotNull()
            .map { calcNameSimilarity(name, it) }
            .max() ?: 0
}

private fun calcNameSimilarity(name: String, expectedName: String): Int {
    val words1 = NameUtil.nameToWordsLowerCase(name)
    val words2 = NameUtil.nameToWordsLowerCase(expectedName)

    val matchedWords = words1.toSet().intersect(words2)
    if (matchedWords.isEmpty()) return 0

    fun isNonNumber(word: String) = !word[0].isDigit()
    val nonNumberWords1 = words1.filter(::isNonNumber)
    val nonNumberWords2 = words2.filter(::isNonNumber)

    // count number of words matched at the end (but ignore number words - they are less important)
    val minWords = Math.min(nonNumberWords1.size, nonNumberWords2.size)
    val matchedTailLength = (0..minWords-1).firstOrNull {
        i -> nonNumberWords1[nonNumberWords1.size - i - 1] != nonNumberWords2[nonNumberWords2.size - i - 1]
    } ?: minWords

    return matchedWords.size * 1000 + matchedTailLength
}
