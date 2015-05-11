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
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.util.ArrayList
import java.util.HashMap
import java.util.LinkedHashMap

public fun JetPsiFactory.createExpressionByPattern(pattern: String, vararg args: Any): JetExpression {
    val (processedText, allPlaceholders) = processPattern(pattern, args)

    var expression = createExpression(processedText)
    val project = expression.getProject()

    val start = expression.startOffset

    val pointerManager = SmartPointerManager.getInstance(project)

    val pointers = HashMap<SmartPsiElementPointer<PsiElement>, Int>()

    PlaceholdersLoop@
    for ((n, placeholders) in allPlaceholders) {
        val expectedElementType = when (args[n]) {
            is String -> continue@PlaceholdersLoop // already in the text
            is JetExpression -> javaClass<JetExpression>()
            is JetTypeReference -> javaClass<JetTypeReference>()
            else -> throw IllegalArgumentException("Unknown argument ${args[n]} - should be JetExpression, JetTypeReference or String")
        }

        for ((range, text) in placeholders) {
            val token = expression.findElementAt(range.getStartOffset())!!
            for (element in token.parents()) {
                val elementRange = element.getTextRange().shiftRight(-start)
                if (elementRange == range && expectedElementType.isInstance(element)) {
                    val pointer = pointerManager.createSmartPsiElementPointer(element)
                    pointers.put(pointer, n)
                    break
                }

                if (!range.contains(elementRange)) {
                    throw IllegalArgumentException("Invalid pattern '$pattern' - no ${expectedElementType.getSimpleName()} found for $$n, text = '$processedText'")
                }
            }
        }
    }

    val codeStyleManager = CodeStyleManager.getInstance(project)

    val stringPlaceholderRanges = allPlaceholders
            .filter { args[it.key] is String }
            .flatMap { it.value }
            .map { it.range }
            .filterNot { it.isEmpty() }
            .sortBy { -it.getStartOffset() }

    // reformat whole text except for String arguments (as they can contain user's formatting to be preserved)
    if (stringPlaceholderRanges.none()) {
        expression = codeStyleManager.reformat(expression, true) as JetExpression
    }
    else {
        var bound = expression.endOffset - 1
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
        element.replace(args[n] as PsiElement)
    }

    codeStyleManager.adjustLineIndent(expression.getContainingFile(), expression.getTextRange())

    return expression
}

private data class Placeholder(val range: TextRange, val text: String)

private data class PatternData(val processedText: String, val placeholders: Map<Int, List<Placeholder>>)

private fun processPattern(pattern: String, args: Array<out Any>): PatternData {
    val ranges = LinkedHashMap<Int, MutableList<Placeholder>>()

    fun charOrNull(i: Int) = if (0 <= i && i < pattern.length()) pattern[i] else null

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
                        if (arg is String) arg else "xyz"
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

    val max = ranges.keySet().max()!!
    for (i in 0..max) {
        check(ranges.contains(i), "no '$$i' placeholder")
    }

    if (args.size() != ranges.size()) {
        throw IllegalArgumentException("Wrong number of arguments, expected: ${ranges.size()}, passed: ${args.size()}")
    }

    return PatternData(text, ranges)
}

public class ExpressionBuilder {
    private val patternBuilder = StringBuilder()
    private val arguments = ArrayList<Any>()

    public fun appendFixedText(text: String): ExpressionBuilder {
        patternBuilder.append(text)
        return this
    }

    public fun appendNonFormattedText(text: String): ExpressionBuilder {
        patternBuilder.append("$" + arguments.size())
        arguments.add(text)
        return this
    }

    public fun appendExpression(expression: JetExpression?): ExpressionBuilder {
        if (expression != null) {
            patternBuilder.append("$" + arguments.size())
            arguments.add(expression)
        }
        return this
    }

    public fun appendTypeReference(typeRef: JetTypeReference?): ExpressionBuilder {
        if (typeRef != null) {
            patternBuilder.append("$" + arguments.size())
            arguments.add(typeRef)
        }
        return this
    }

    public fun createExpression(factory: JetPsiFactory): JetExpression {
        return factory.createExpressionByPattern(patternBuilder.toString(), *arguments.toArray())
    }
}

public fun JetPsiFactory.buildExpression(build: ExpressionBuilder.() -> Unit): JetExpression {
    val builder = ExpressionBuilder()
    builder.build()
    return builder.createExpression(this)
}
