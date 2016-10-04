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
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.psiUtil.PsiChildRange
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.renderer.render
import java.util.*

fun KtPsiFactory.createExpressionByPattern(pattern: String, vararg args: Any): KtExpression
        = createByPattern(pattern, *args) { createExpression(it) }

fun KtPsiFactory.createValueArgumentListByPattern(pattern: String, vararg args: Any): KtValueArgumentList
        = createByPattern(pattern, *args) { createCallArguments(it) }

fun <TDeclaration : KtDeclaration> KtPsiFactory.createDeclarationByPattern(pattern: String, vararg args: Any): TDeclaration
        = createByPattern(pattern, *args) { createDeclaration<TDeclaration>(it) }

fun KtPsiFactory.createDestructuringDeclarationByPattern(pattern: String, vararg args: Any): KtDestructuringDeclaration
        = createByPattern(pattern, *args) { createDestructuringDeclaration(it) }

private abstract class ArgumentType<T : Any>(val klass: Class<T>)

private class PlainTextArgumentType<T : Any>(klass: Class<T>, val toPlainText: (T) -> String) : ArgumentType<T>(klass)

private abstract class PsiElementPlaceholderArgumentType<T : Any, TPlaceholder : PsiElement>(klass: Class<T>, val placeholderClass: Class<TPlaceholder>) : ArgumentType<T>(klass) {
    abstract fun replacePlaceholderElement(placeholder: TPlaceholder, argument: T): PsiChildRange
}

private class PsiElementArgumentType<T : PsiElement>(klass: Class<T>) : PsiElementPlaceholderArgumentType<T, T>(klass, klass) {
    override fun replacePlaceholderElement(placeholder: T, argument: T): PsiChildRange {
        // if argument element has generated flag then it has not been formatted yet and we should do this manually
        // (because we cleared this flag for the whole tree above and PostprocessReformattingAspect won't format anything)
        val reformat = CodeEditUtil.isNodeGenerated(argument.node)
        var result = placeholder.replace(argument)
        if (reformat) {
            result = CodeStyleManager.getInstance(result.project).reformat(result, true)
        }
        return PsiChildRange.singleElement(result)
    }
}

private object PsiChildRangeArgumentType : PsiElementPlaceholderArgumentType<PsiChildRange, KtElement>(PsiChildRange::class.java, KtElement::class.java) {
    override fun replacePlaceholderElement(placeholder: KtElement, argument: PsiChildRange): PsiChildRange {
        val project = placeholder.project
        val codeStyleManager = CodeStyleManager.getInstance(project)

        if (argument.isEmpty) {
            placeholder.delete()
            return PsiChildRange.EMPTY
        }
        else {
            val first = placeholder.parent.addRangeBefore(argument.first!!, argument.last!!, placeholder)
            val last = placeholder.prevSibling
            placeholder.delete()

            codeStyleManager.reformatNewlyAddedElement(first.node.treeParent, first.node)
            if (last != first) {
                codeStyleManager.reformatNewlyAddedElement(last.node.treeParent, last.node)
            }
            return PsiChildRange(first, last)
        }
    }
}

private val SUPPORTED_ARGUMENT_TYPES = listOf(
        PsiElementArgumentType<KtExpression>(KtExpression::class.java),
        PsiElementArgumentType<KtTypeReference>(KtTypeReference::class.java),
        PlainTextArgumentType<String>(String::class.java, toPlainText = { it }),
        PlainTextArgumentType<Name>(Name::class.java, toPlainText = { it.render() }),
        PsiChildRangeArgumentType
)

fun <TElement : KtElement> createByPattern(pattern: String, vararg args: Any, factory: (String) -> TElement): TElement {
    val argumentTypes = args.map { arg ->
        SUPPORTED_ARGUMENT_TYPES.firstOrNull { it.klass.isInstance(arg) }
            ?: throw IllegalArgumentException("Unsupported argument type: ${arg.javaClass}, should be one of: ${SUPPORTED_ARGUMENT_TYPES.map { it.klass.simpleName }.joinToString()}")
    }

    // convert arguments that can be converted into plain text
    @Suppress("NAME_SHADOWING")
    val args = args.zip(argumentTypes).map {
        val (arg, type) = it
        if (type is PlainTextArgumentType)
            (type.toPlainText as Function1<in Any, String>).invoke(arg) // TODO: see KT-7833
        else
            arg
    }

    val (processedText, allPlaceholders) = processPattern(pattern, args)

    var resultElement = factory(processedText.trim())
    val project = resultElement.project

    val start = resultElement.startOffset

    val pointerManager = SmartPointerManager.getInstance(project)

    val pointers = HashMap<SmartPsiElementPointer<PsiElement>, Int>()

    PlaceholdersLoop@
    for ((n, placeholders) in allPlaceholders) {
        val arg = args[n]
        if (arg is String) continue // already in the text
        val expectedElementType = (argumentTypes[n] as PsiElementPlaceholderArgumentType<*, *>).placeholderClass

        for ((range, text) in placeholders) {
            val token = resultElement.findElementAt(range.startOffset)!!
            for (element in token.parentsWithSelf) {
                val elementRange = element.getTextRange().shiftRight(-start)
                if (elementRange == range && expectedElementType.isInstance(element)) {
                    val pointer = pointerManager.createSmartPsiElementPointer(element)
                    pointers.put(pointer, n)
                    break
                }

                if (!range.contains(elementRange)) {
                    throw IllegalArgumentException("Invalid pattern '$pattern' - no ${expectedElementType.simpleName} found for $$n, text = '$processedText'")
                }
            }
        }
    }

    val codeStyleManager = CodeStyleManager.getInstance(project)

    val stringPlaceholderRanges = allPlaceholders
            .filter { args[it.key] is String }
            .flatMap { it.value }
            .map { it.range }
            .filterNot { it.isEmpty }
            .sortedByDescending { it.startOffset }

    // reformat whole text except for String arguments (as they can contain user's formatting to be preserved)
    if (stringPlaceholderRanges.none()) {
        resultElement = codeStyleManager.reformat(resultElement, true) as TElement
    }
    else {
        var bound = resultElement.endOffset - 1
        for (range in stringPlaceholderRanges) {
            // we extend reformatting range by 1 to the right because otherwise some of spaces are not reformatted
            resultElement = codeStyleManager.reformatRange(resultElement, range.endOffset + start, bound + 1, true) as TElement
            bound = range.startOffset + start
        }
        resultElement = codeStyleManager.reformatRange(resultElement, start, bound + 1, true) as TElement
    }

    // do not reformat the whole expression in PostprocessReformattingAspect
    CodeEditUtil.setNodeGeneratedRecursively(resultElement.node, false)

    for ((pointer, n) in pointers) {
        var element = pointer.element!!
        if (element is KtFunctionLiteral) {
            element = element.getParent() as KtLambdaExpression
        }
        @Suppress("UNCHECKED_CAST")
        val argumentType = argumentTypes[n] as PsiElementPlaceholderArgumentType<in Any, in PsiElement>
        val range = argumentType.replacePlaceholderElement(element, args[n])

        if (element == resultElement) {
            assert(range.first == range.last)
            resultElement = range.first as TElement
        }
    }

    codeStyleManager.adjustLineIndent(resultElement.containingFile, resultElement.textRange)

    return resultElement
}

private data class Placeholder(val range: TextRange, val text: String)

private data class PatternData(val processedText: String, val placeholders: Map<Int, List<Placeholder>>)

private fun processPattern(pattern: String, args: List<Any>): PatternData {
    val ranges = LinkedHashMap<Int, MutableList<Placeholder>>()

    fun charOrNull(i: Int) = if (0 <= i && i < pattern.length) pattern[i] else null

    fun check(condition: Boolean, message: String) {
        if (!condition) {
            throw IllegalArgumentException("Invalid pattern '$pattern' - $message")
        }
    }

    val text = buildString {
        var i = 0
        while (i < pattern.length) {
            var c = pattern[i]

            if (c == '$') {
                val nextChar = charOrNull(++i)
                if (nextChar == '$') {
                    append(nextChar)
                }
                else {
                    check(nextChar?.isDigit() ?: false, "unclosed '$'")

                    val lastIndex = (i..pattern.length - 1).firstOrNull { !pattern[it].isDigit() } ?: pattern.length
                    val n = pattern.substring(i, lastIndex).toInt()
                    check(n >= 0, "invalid placeholder number: $n")
                    i = lastIndex

                    val arg: Any? = if (n < args.size) args[n] else null /* report wrong number of arguments later */
                    val placeholderText = if (charOrNull(i) != ':' || charOrNull(i + 1) != '\'') {
                        if (arg is String) arg else "xyz"
                    }
                    else {
                        check(arg !is String, "do not specify placeholder text for $$n - plain text argument passed")
                        i += 2 // skip ':' and '\''
                        val endIndex = pattern.indexOf('\'', i)
                        check(endIndex >= 0, "unclosed placeholder text")
                        check(endIndex > i, "empty placeholder text")
                        val text = pattern.substring(i, endIndex)
                        i = endIndex + 1
                        text
                    }

                    append(placeholderText)
                    val range = TextRange(length - placeholderText.length, length)
                    ranges.getOrPut(n, { ArrayList() }).add(Placeholder(range, placeholderText))
                    continue
                }
            }
            else {
                append(c)
            }
            i++
        }
    }

    if (!ranges.isEmpty()) {
        val max = ranges.keys.max()!!
        for (i in 0..max) {
            check(ranges.contains(i), "no '$$i' placeholder")
        }
    }

    if (args.size != ranges.size) {
        throw IllegalArgumentException("Wrong number of arguments, expected: ${ranges.size}, passed: ${args.size}")
    }

    return PatternData(text, ranges)
}

class BuilderByPattern<TElement> {
    private val patternBuilder = StringBuilder()
    private val arguments = ArrayList<Any>()

    fun appendFixedText(text: String): BuilderByPattern<TElement> {
        patternBuilder.append(text)
        return this
    }

    fun appendNonFormattedText(text: String): BuilderByPattern<TElement> {
        patternBuilder.append("$" + arguments.size)
        arguments.add(text)
        return this
    }

    fun appendExpression(expression: KtExpression?): BuilderByPattern<TElement> {
        if (expression != null) {
            patternBuilder.append("$" + arguments.size)
            arguments.add(expression)
        }
        return this
    }

    fun appendExpressions(expressions: Iterable<KtExpression?>, separator: String = ","): BuilderByPattern<TElement> {
        for ((index, expression) in expressions.withIndex()) {
            if (index > 0) {
                appendFixedText(separator)
            }
            appendExpression(expression)
        }
        return this
    }

    fun appendTypeReference(typeRef: KtTypeReference?): BuilderByPattern<TElement> {
        if (typeRef != null) {
            patternBuilder.append("$" + arguments.size)
            arguments.add(typeRef)
        }
        return this
    }

    fun appendName(name: Name): BuilderByPattern<TElement> {
        patternBuilder.append("$" + arguments.size)
        arguments.add(name)
        return this
    }

    fun appendChildRange(range: PsiChildRange): BuilderByPattern<TElement> {
        patternBuilder.append("$" + arguments.size)
        arguments.add(range)
        return this
    }

    fun create(factory: (String, Array<out Any>) -> TElement): TElement {
        return factory(patternBuilder.toString(), arguments.toArray())
    }
}

fun KtPsiFactory.buildExpression(build: BuilderByPattern<KtExpression>.() -> Unit): KtExpression {
    return buildByPattern({ pattern, args -> this.createExpressionByPattern(pattern, *args) }, build)
}

fun KtPsiFactory.buildValueArgumentList(build: BuilderByPattern<KtValueArgumentList>.() -> Unit): KtValueArgumentList {
    return buildByPattern({ pattern, args -> this.createValueArgumentListByPattern(pattern, *args) }, build)
}

fun KtPsiFactory.buildDeclaration(build: BuilderByPattern<KtDeclaration>.() -> Unit): KtDeclaration {
    return buildByPattern({ pattern, args -> this.createDeclarationByPattern(pattern, *args) }, build)
}

fun KtPsiFactory.buildDestructuringDeclaration(build: BuilderByPattern<KtDestructuringDeclaration>.() -> Unit): KtDestructuringDeclaration {
    return buildByPattern({ pattern, args -> this.createDestructuringDeclarationByPattern(pattern, *args) }, build)
}

fun <TElement> buildByPattern(factory: (String, Array<out Any>) -> TElement, build: BuilderByPattern<TElement>.() -> Unit): TElement {
    val builder = BuilderByPattern<TElement>()
    builder.build()
    return builder.create(factory)
}
