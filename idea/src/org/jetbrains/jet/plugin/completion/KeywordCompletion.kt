/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.completion

import com.intellij.psi.filters.*
import com.intellij.psi.filters.position.LeftNeighbour
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.lexer.JetTokens
import com.intellij.psi.PsiElement
import com.intellij.psi.filters.position.PositionElementFilter
import com.intellij.codeInsight.completion.*
import org.jetbrains.jet.plugin.completion.handlers.JetFunctionInsertHandler
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiErrorElement
import org.jetbrains.jet.lexer.JetKeywordToken
import com.intellij.openapi.project.Project
import org.jetbrains.jet.lang.psi.psiUtil.getParentByType
import org.jetbrains.jet.lang.psi.psiUtil.prevLeaf
import org.jetbrains.jet.plugin.completion.handlers.KotlinKeywordInsertHandler
import org.jetbrains.jet.lang.psi.psiUtil.siblings
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.PsiComment

class KeywordLookupObject(val keyword: String)

object KeywordCompletion {
    private val NON_ACTUAL_KEYWORDS = setOf(JetTokens.REIFIED_KEYWORD.getValue(),
                                            JetTokens.CAPITALIZED_THIS_KEYWORD.getValue(),
                                            JetTokens.TYPE_ALIAS_KEYWORD.getValue())
    private val ALL_KEYWORDS = (JetTokens.KEYWORDS.getTypes() + JetTokens.SOFT_KEYWORDS.getTypes())
            .map { (it as JetKeywordToken).getValue() }
            .filter { it !in NON_ACTUAL_KEYWORDS }

    public fun complete(parameters: CompletionParameters, prefixMatcher: PrefixMatcher, collector: LookupElementsCollector) {
        val position = parameters.getPosition()

        if (!GENERAL_FILTER.isAcceptable(position, position)) return

        val parserFilter = buildFilter(position)
        for (keyword in ALL_KEYWORDS) {
            if (prefixMatcher.prefixMatches(keyword) && parserFilter(keyword)) {
                val element = LookupElementBuilder.create(KeywordLookupObject(keyword), keyword)
                        .bold()
                        .withInsertHandler(if (keyword !in FUNCTION_KEYWORDS)
                                               KotlinKeywordInsertHandler
                                           else
                                               JetFunctionInsertHandler.NO_PARAMETERS_HANDLER)
                collector.addElement(element)
            }
        }
    }

    private val FUNCTION_KEYWORDS = listOf(JetTokens.GET_KEYWORD.toString(), JetTokens.SET_KEYWORD.toString())

    private val GENERAL_FILTER = NotFilter(OrFilter(
            CommentFilter(),
            ParentFilter(ClassFilter(javaClass<JetLiteralStringTemplateEntry>())),
            ParentFilter(ClassFilter(javaClass<JetConstantExpression>())),
            LeftNeighbour(TextFilter(".")),
            LeftNeighbour(TextFilter("?."))
    ))

    private class CommentFilter() : ElementFilter {
        override fun isAcceptable(element : Any?, context : PsiElement?)
                = (element is PsiElement) && JetPsiUtil.isInComment(element as PsiElement)

        override fun isClassAcceptable(hintClass: Class<out Any?>)
                = true
    }

    private class ParentFilter(filter : ElementFilter) : PositionElementFilter() {
        {
            setFilter(filter)
        }

        override fun isAcceptable(element : Any?, context : PsiElement?) : Boolean {
            val parent = (element as? PsiElement)?.getParent()
            return parent != null && (getFilter()?.isAcceptable(parent, context) ?: true)
        }
    }

    private fun buildFilter(position: PsiElement): (String) -> Boolean {
        var parent = position.getParent()
        var prevParent = position
        while (parent != null) {
            val _parent = parent
            when (_parent) {
                is JetBlockExpression -> {
                    return buildFilterWithContext("fun foo() { ", prevParent, position)
                }

                is JetWithExpressionInitializer -> {
                    val initializer = _parent.getInitializer()
                    if (prevParent == initializer) {
                        return buildFilterWithContext("val v = ", initializer, position)
                    }
                }

                is JetParameter -> {
                    val default = _parent.getDefaultValue()
                    if (prevParent == default) {
                        return buildFilterWithContext("val v = ", default, position)
                    }
                }
            }

            if (_parent is JetDeclaration) {
                val scope = _parent.getParent()
                when (scope) {
                    is JetClassOrObject -> return buildFilterWithReducedContext("class X { ", _parent, position)
                    is JetFile -> return buildFilterWithReducedContext("", _parent, position)
                }
            }

            prevParent = _parent
            parent = _parent.getParent()
        }

        return buildFilterWithReducedContext("", null, position)
    }

    private fun buildFilterWithContext(prefixText: String,
                                       contextElement: PsiElement,
                                       position: PsiElement): (String) -> Boolean {
        val offset = position.getStartOffsetInAncestor(contextElement)
        val truncatedContext = contextElement.getText()!!.substring(0, offset)
        return buildFilterByText(prefixText + truncatedContext, contextElement.getProject())
    }

    private fun buildFilterWithReducedContext(prefixText: String,
                                              contextElement: PsiElement?,
                                              position: PsiElement): (String) -> Boolean {
        val builder = StringBuilder()
        buildReducedContextBefore(builder, position, contextElement)
        return buildFilterByText(prefixText + builder.toString(), position.getProject())
    }


    private fun buildFilterByText(prefixText: String, project: Project): (String) -> Boolean {
        val psiFactory = JetPsiFactory(project)
        return { keyword ->
            val file = psiFactory.createFile(prefixText + keyword)
            val elementAt = file.findElementAt(prefixText.length)!!
            val nodeType = elementAt.getNode()!!.getElementType()
            when {
                nodeType !in JetTokens.KEYWORDS && nodeType !in JetTokens.SOFT_KEYWORDS -> false

                elementAt.getParentByType(javaClass<PsiErrorElement>(), strict = false) != null -> false

                elementAt.prevLeafSkipWhitespacesAndComments() is PsiErrorElement -> false

                else -> true
            }
        }
    }

    private fun PsiElement.prevLeafSkipWhitespacesAndComments(): PsiElement? {
        var leaf = prevLeaf()
        while (leaf != null) {
            if (leaf !is PsiWhiteSpace && leaf !is PsiComment) return leaf
            leaf = leaf!!.prevLeaf()
        }
        return null
    }

    // builds text within scope (or from the start of the file) before position element excluding almost all declarations
    private fun buildReducedContextBefore(builder: StringBuilder, position: PsiElement, scope: PsiElement?) {
        if (position == scope) return
        val parent = position.getParent() ?: return

        buildReducedContextBefore(builder, parent, scope)

        val prevDeclaration = position.siblings(forward = false, withItself = false).firstOrNull { it is JetDeclaration }

        var child = parent.getFirstChild()
        while (child != position) {
            if (child is JetDeclaration) {
                if (child == prevDeclaration) {
                    builder.appendReducedText(child!!)
                }
            }
            else {
                builder.append(child!!.getText())
            }

            child = child!!.getNextSibling()
        }
    }

    private fun StringBuilder.appendReducedText(element: PsiElement) {
        var child = element.getFirstChild()
        if (child == null) {
            append(element.getText()!!)
        }
        else {
            while (child != null) {
                when (child) {
                    is JetBlockExpression, is JetClassBody -> append("{}")
                    else -> appendReducedText(child)
                }

                child = child.getNextSibling()
            }
        }
    }

    private fun PsiElement.getStartOffsetInAncestor(ancestor: PsiElement): Int {
        val parent = getParent()!!
        return if (parent == ancestor)
            getStartOffsetInParent()
        else
            parent.getStartOffsetInAncestor(ancestor) + getStartOffsetInParent()
    }
}
