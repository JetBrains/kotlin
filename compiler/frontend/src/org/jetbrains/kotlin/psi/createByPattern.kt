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

package org.jetbrains.kotlin.psi

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.impl.source.codeStyle.CodeEditUtil
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.util.ArrayList
import java.util.HashMap
import java.util.LinkedHashMap

public fun JetPsiFactory.createExpressionByPattern(pattern: String, vararg args: Any): JetExpression {
    val (processedText, allPlaceholders) = processPattern(pattern, args)

    var expression = createExpression(processedText)
    val project = expression.getProject()

    val start = expression.getTextRange().getStartOffset()

    val pointerManager = SmartPointerManager.getInstance(project)

    val pointers = HashMap<SmartPsiElementPointer<JetExpression>, Int>()
    for ((n, placeholders) in allPlaceholders) {
        if (args[n] is String) continue // already in the text

        for ((range, text) in placeholders) {
            val token = expression.findElementAt(range.getStartOffset())!!
            for (element in token.parents()) {
                val elementRange = element.getTextRange().shiftRight(-start)
                if (elementRange == range) {
                    val elementToUse = element.parents(withItself = false).firstIsInstanceOrNull<JetExpression>()
                                       ?: error("Invalid pattern - no JetExpression found for $n")
                    val pointer = pointerManager.createSmartPsiElementPointer(elementToUse)
                    pointers.put(pointer, n)
                    break
                }
                else if (!range.contains(elementRange)) {
                    throw IllegalArgumentException("Invalid pattern - no PsiElement found for $$n")
                }
            }
        }
    }

    val codeStyleManager = CodeStyleManager.getInstance(project)

    val stringPlaceholderRanges = allPlaceholders
            .filter { args[it.getKey()] is String }
            .flatMap { it.getValue() }
            .map { it.range }
            .sortBy { -it.getStartOffset() }

    // reformat whole text except for String arguments (as they can contain user's formatting to be preserved)
    if (stringPlaceholderRanges.none()) {
        expression = codeStyleManager.reformat(expression, true) as JetExpression
    }
    else {
        var bound = expression.getTextRange().getEndOffset() - 1
        for (range in stringPlaceholderRanges) {
            // we extend reformatting range by 1 to the right because otherwise some of spaces are not reformatted
            expression = codeStyleManager.reformatRange(expression, range.getEndOffset() + start, bound + 1, true) as JetExpression
            bound = range.getStartOffset() + start
        }
        expression = codeStyleManager.reformatRange(expression, start, bound + 1, true) as JetExpression
    }

    // do not reformat the whole expression in PostprocessReformattingAspect
    CodeEditUtil.setNodeGeneratedRecursively(expression.getNode(), false)

    for ((pointer, n) in pointers) {
        val element = pointer.getElement()!!
        val arg = args[n]
        if (arg !is JetExpression) {
            throw IllegalArgumentException("Unknown argument $arg - should be JetExpression or String")
        }
        element.replace(arg)
    }

    codeStyleManager.adjustLineIndent(expression.getContainingFile(), expression.getTextRange())

    return expression
}

private data class Placeholder(val range: TextRange, val text: String)

private data class PatternData(val processedText: String, val placeholders: Map<Int, List<Placeholder>>)

private fun processPattern(pattern: String, args: Array<out Any>): PatternData {
    val ranges = LinkedHashMap<Int, MutableList<Placeholder>>()

    fun charOrNull(i: Int) = if (i < pattern.length()) pattern[i] else null

    fun check(condition: Boolean, message: String) {
        if (!condition) {
            throw IllegalArgumentException("Invalid pattern '$pattern' - $message")
        }
    }

    val text = StringBuilder {
        var i = 0
        while (i < pattern.length()) {
            var c = pattern[i]

            if (c == '$') {
                val nextChar = charOrNull(++i)
                if (nextChar == '$') {
                    append(nextChar)
                }
                else {
                    check(nextChar?.isDigit() ?: false, "unclosed '$'")

                    val lastIndex = (i..pattern.length() - 1).firstOrNull { !pattern[it].isDigit() } ?: pattern.length()
                    val n = pattern.substring(i, lastIndex).toInt()
                    check(n >= 0, "invalid placeholder number: $n")
                    i = lastIndex

                    val arg: Any? = if (n < args.size()) args[n] else null /* report wrong number of arguments later */
                    val placeholderText = if (charOrNull(i) != '=') {
                        arg as? String ?: "xyz"
                    }
                    else {
                        check(arg !is String, "do not specify placeholder text for $$n - String argument passed")
                        i++
                        val endIndex = pattern.indexOf('$', i)
                        check(endIndex >= 0, "unclosed placeholder text")
                        check(endIndex > i, "empty placeholder text")
                        val text = pattern.substring(i, endIndex)
                        i = endIndex + 1
                        text
                    }

                    append(placeholderText)
                    val range = TextRange(length() - placeholderText.length(), length())
                    ranges.getOrPut(n, { ArrayList() }).add(Placeholder(range, placeholderText))
                    continue
                }
            }
            else {
                append(c)
            }
            i++
        }
    }.toString()

    check(!ranges.isEmpty(), "no placeholders found")

    val max = ranges.keySet().max()!!
    for (i in 0..max) {
        check(ranges.contains(i), "no '$$i' placeholder")
    }

    if (args.size() != ranges.size()) {
        throw IllegalArgumentException("Wrong number of arguments, expected: ${ranges.size()}, passed: ${args.size()}")
    }

    return PatternData(text, ranges)
}
