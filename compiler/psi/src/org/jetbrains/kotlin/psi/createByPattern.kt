/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.psiUtil.PsiChildRange
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.renderer.render

fun KtPsiFactory.createExpressionByPattern(@NonNls pattern: String, @NonNls vararg args: Any, reformat: Boolean = true): KtExpression =
    createByPattern(pattern, *args, reformat = reformat) { createExpression(it) }

fun KtPsiFactory.createValueArgumentListByPattern(
    @NonNls pattern: String,
    @NonNls vararg args: Any,
    reformat: Boolean = true
): KtValueArgumentList = createByPattern(pattern, *args, reformat = reformat) { createCallArguments(it) }

fun <TDeclaration : KtDeclaration> KtPsiFactory.createDeclarationByPattern(
    @NonNls pattern: String,
    @NonNls vararg args: Any,
    reformat: Boolean = true
): TDeclaration = createByPattern(pattern, *args, reformat = reformat) { createDeclaration(it) }

fun KtPsiFactory.createDestructuringDeclarationByPattern(
    @NonNls pattern: String,
    @NonNls vararg args: Any,
    reformat: Boolean = true
): KtDestructuringDeclaration = createByPattern(pattern, *args, reformat = reformat) { createDestructuringDeclaration(it) }

private abstract class ArgumentType<T : Any>(val klass: Class<T>)

private class PlainTextArgumentType<T : Any>(klass: Class<T>, val toPlainText: (T) -> String) : ArgumentType<T>(klass)

private abstract class PsiElementPlaceholderArgumentType<T : Any, TPlaceholder : PsiElement>(
    klass: Class<T>,
    val placeholderClass: Class<TPlaceholder>
) : ArgumentType<T>(klass) {
    abstract fun replacePlaceholderElement(placeholder: TPlaceholder, argument: T, reformat: Boolean): PsiChildRange
}

private class PsiElementArgumentType<T : PsiElement>(klass: Class<T>) : PsiElementPlaceholderArgumentType<T, T>(klass, klass) {
    override fun replacePlaceholderElement(placeholder: T, argument: T, reformat: Boolean): PsiChildRange {
        var result = if (placeholder is KtExpressionImplStub<*>) {
            KtExpressionImpl.replaceExpression(placeholder, argument, reformat, placeholder::rawReplace)
        } else {
            placeholder.replace(argument)
        }
        // if argument element has generated flag then it has not been formatted yet and we should do this manually
        // (because we cleared this flag for the whole tree above and PostprocessReformattingAspect won't format anything)
        if (CodeEditUtil.isNodeGenerated(argument.node)) {
            result = CodeStyleManager.getInstance(result.project).reformat(result, true)
        }
        return PsiChildRange.singleElement(result)
    }
}

private object PsiChildRangeArgumentType :
    PsiElementPlaceholderArgumentType<PsiChildRange, KtElement>(PsiChildRange::class.java, KtElement::class.java) {
    override fun replacePlaceholderElement(placeholder: KtElement, argument: PsiChildRange, reformat: Boolean): PsiChildRange {
        val project = placeholder.project

        return if (!argument.isEmpty) {
            val first = placeholder.parent.addRangeBefore(argument.first!!, argument.last!!, placeholder)
            val last = placeholder.prevSibling
            placeholder.delete()

            if (reformat) {
                val codeStyleManager = CodeStyleManager.getInstance(project)
                codeStyleManager.reformatNewlyAddedElement(first.node.treeParent, first.node)
                if (last != first) {
                    codeStyleManager.reformatNewlyAddedElement(last.node.treeParent, last.node)
                }
            }
            PsiChildRange(first, last)
        } else {
            placeholder.delete()
            PsiChildRange.EMPTY
        }
    }
}

private val SUPPORTED_ARGUMENT_TYPES = listOf(
    PsiElementArgumentType(KtExpression::class.java),
    PsiElementArgumentType(KtTypeReference::class.java),
    PlainTextArgumentType(String::class.java, toPlainText = { it }),
    PlainTextArgumentType(Name::class.java, toPlainText = Name::render),
    PsiChildRangeArgumentType
)

@get:TestOnly
@set:TestOnly
var CREATE_BY_PATTERN_MAY_NOT_REFORMAT = false

fun <TElement : KtElement> createByPattern(
    pattern: String,
    vararg args: Any,
    reformat: Boolean = true,
    factory: (String) -> TElement
): TElement {
    val argumentTypes = args.map { arg ->
        SUPPORTED_ARGUMENT_TYPES.firstOrNull { it.klass.isInstance(arg) }
            ?: throw IllegalArgumentException(
                "Unsupported argument type: ${arg::class.java}, should be one of: " +
                        SUPPORTED_ARGUMENT_TYPES.joinToString { it.klass.simpleName }
            )
    }

    // convert arguments that can be converted into plain text
    @Suppress("NAME_SHADOWING")
    val args = args.zip(argumentTypes).map {
        val (arg, type) = it
        if (type is PlainTextArgumentType)
            @Suppress("UNCHECKED_CAST") // Suppress compiler checks as types checks were done in runtime
            (type.toPlainText as Function1<Any, String>).invoke(arg)
        else
            arg
    }

    val (processedText, allPlaceholders) = processPattern(pattern, args)

    var resultElement: KtElement = factory(processedText.trim())
    val project = resultElement.project

    val start = resultElement.startOffset

    val pointerManager = SmartPointerManager.getInstance(project)

    val pointers = LinkedHashMap<SmartPsiElementPointer<PsiElement>, Int>()

    PlaceholdersLoop@
    for ((n, placeholders) in allPlaceholders) {
        val arg = args[n]
        if (arg is String) continue // already in the text
        val expectedElementType = (argumentTypes[n] as PsiElementPlaceholderArgumentType<*, *>).placeholderClass

        for ((range, _) in placeholders) {
            val token = resultElement.findElementAt(range.startOffset)!!
            for (element in token.parentsWithSelf) {
                val elementRange = element.textRange.shiftRight(-start)
                if (elementRange == range && expectedElementType.isInstance(element)) {
                    val pointer = pointerManager.createSmartPsiElementPointer(element)
                    pointers[pointer] = n
                    break
                }

                if (!range.contains(elementRange)) {
                    throw IllegalArgumentException("Invalid pattern '$pattern' - no ${expectedElementType.simpleName} found for $$n, text = '$processedText'")
                }
            }
        }
    }

    val codeStyleManager = CodeStyleManager.getInstance(project)

    if (reformat) {
        if (CREATE_BY_PATTERN_MAY_NOT_REFORMAT) {
            throw java.lang.IllegalArgumentException("Reformatting is not allowed in the current context; please change the invocation to use reformat=false")
        }
        val stringPlaceholderRanges = allPlaceholders.asSequence()
            .filter { args[it.key] is String }
            .flatMap { it.value }
            .map { it.range }
            .filterNot { it.isEmpty }
            .sortedByDescending { it.startOffset }

        // reformat whole text except for String arguments (as they can contain user's formatting to be preserved)
        resultElement = if (stringPlaceholderRanges.none()) {
            codeStyleManager.reformat(resultElement, true) as KtElement
        } else {
            var bound = resultElement.endOffset - 1
            for (range in stringPlaceholderRanges) {
                // we extend reformatting range by 1 to the right because otherwise some of spaces are not reformatted
                resultElement = codeStyleManager.reformatRange(resultElement, range.endOffset + start, bound + 1, true) as KtElement
                bound = range.startOffset + start
            }
            codeStyleManager.reformatRange(resultElement, start, bound + 1, true) as KtElement
        }

        // do not reformat the whole expression in PostprocessReformattingAspect
        CodeEditUtil.setNodeGeneratedRecursively(resultElement.node, false)
    }

    // it is needed to place logical operators first for expressions like `xyz xyz xyz` to become `xyz && xyz`
    // otherwise if when we place an expression we have to wrap it with extra parenthesis => it becomes `(a != null) xyz xyz`
    val (left, right) = pointers.entries.partition {
        val n = it.value
        val elementType = (args[n] as? KtOperationReferenceExpression)?.getReferencedNameElementType()
        elementType == KtTokens.ANDAND || elementType == KtTokens.OROR
    }

    for (partition in listOf(left, right)) {
        for ((pointer, n) in partition) {
            var element = pointer.element!!

            if (element is KtFunctionLiteral) {
                element = element.parent as KtLambdaExpression
            }
            @Suppress("UNCHECKED_CAST")
            val argumentType = argumentTypes[n] as PsiElementPlaceholderArgumentType<in Any, in PsiElement>
            val range = argumentType.replacePlaceholderElement(element, args[n], reformat)

            if (element == resultElement) {
                assert(range.first == range.last)
                resultElement = range.first as KtElement
            }
        }
    }

    if (reformat)
        codeStyleManager.adjustLineIndent(resultElement.containingFile, resultElement.textRange)

    @Suppress("UNCHECKED_CAST")
    return resultElement as TElement
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
            val c = pattern[i]

            if (c == '$') {
                val nextChar = charOrNull(++i)
                if (nextChar == '$') {
                    append(nextChar)
                } else {
                    check(nextChar?.isDigit() ?: false, "unclosed '$'")

                    val lastIndex = (i until pattern.length).firstOrNull { !pattern[it].isDigit() } ?: pattern.length
                    val n = pattern.substring(i, lastIndex).toInt()
                    check(n >= 0, "invalid placeholder number: $n")
                    i = lastIndex

                    val arg: Any? = if (n < args.size) args[n] else null /* report wrong number of arguments later */
                    val placeholderText = if (charOrNull(i) != ':' || charOrNull(i + 1) != '\'') {
                        arg as? String ?: "xyz"
                    } else {
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
                    ranges.getOrPut(n) { ArrayList() }.add(Placeholder(range, placeholderText))
                    continue
                }
            } else {
                append(c)
            }
            i++
        }
    }

    if (!ranges.isEmpty()) {
        val max = ranges.keys.maxOrNull()!!
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

fun KtPsiFactory.buildExpression(reformat: Boolean = true, build: BuilderByPattern<KtExpression>.() -> Unit): KtExpression {
    return buildByPattern({ pattern, args -> this.createExpressionByPattern(pattern, *args, reformat = reformat) }, build)
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
