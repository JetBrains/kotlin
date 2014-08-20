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
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.filters.position.LeftNeighbour
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.lexer.JetToken
import org.jetbrains.jet.lexer.JetTokens
import com.intellij.psi.filters.position.SuperParentFilter
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.filters.position.PositionElementFilter
import com.intellij.util.containers.MultiMap
import java.util.HashSet
import com.intellij.codeInsight.completion.*
import org.jetbrains.jet.plugin.completion.handlers.JetFunctionInsertHandler
import org.jetbrains.jet.plugin.completion.handlers.JetKeywordInsertHandler
import com.intellij.codeInsight.lookup.LookupElementBuilder
import java.util.ArrayList

class KeywordLookupObject(val keyword: String)

//TODO: it should be object, it's class because of KT-5582
class KeywordCompletion {
    public fun complete(parameters: CompletionParameters, collector: LookupElementsCollector) {
        val position = parameters.getPosition()

        if (!GENERAL_FILTER.isAcceptable(position, position)) return

        for ((filter, keywords) in filterToKeyword) {
            if (filter.isAcceptable(position, position)) {
                for (keyword in keywords) {
                    val element = LookupElementBuilder.create(KeywordLookupObject(keyword), keyword)
                            .bold()
                            .withInsertHandler(if (keyword !in FUNCTION_KEYWORDS)
                                                   JetKeywordInsertHandler
                                               else
                                                   JetFunctionInsertHandler.NO_PARAMETERS_HANDLER)
                    collector.addElement(element)
                }
            }
        }
    }

    private val FUNCTION_KEYWORDS = listOf(JetTokens.GET_KEYWORD.toString(), JetTokens.SET_KEYWORD.toString())

    private val NOT_IDENTIFIER_FILTER = NotFilter(AndFilter(
            LeafElementFilter(JetTokens.IDENTIFIER),
            NotFilter(ParentFilter(ClassFilter(javaClass<JetReferenceExpression>())))
    ))

    private val GENERAL_FILTER = NotFilter(OrFilter(
            CommentFilter(),
            ParentFilter(ClassFilter(javaClass<JetLiteralStringTemplateEntry>())),
            ParentFilter(ClassFilter(javaClass<JetConstantExpression>())),
            LeftNeighbour(TextFilter("."))
    ))

    private fun notIdentifier(filter: ElementFilter) = AndFilter(NOT_IDENTIFIER_FILTER, filter)

    private val filterToKeyword: List<Pair<ElementFilter, Collection<String>>> = run {
        val inTopLevel = notIdentifier(InTopFilter)
        val inClassBody = notIdentifier(InClassBodyFilter())
        val inNonClassBlock = notIdentifier(InNonClassBlockFilter)
        val inPropertyBody = notIdentifier(InPropertyBodyFilter)

        val inNonParameterModifier = notIdentifier(AndFilter(
                SuperParentFilter(ClassFilter(javaClass<JetModifierList>())),
                NotFilter(InTypeParameterFirstChildFilter)
        ))

        val filtersToKeywords : MultiMap<HashSet<ElementFilter>, String> = MultiMap.create()

        fun add(keyword: JetToken, vararg filters: ElementFilter) {
            filtersToKeywords.putValue(hashSetOf(*filters), keyword.toString())
        }

        add(JetTokens.ABSTRACT_KEYWORD, inTopLevel, inNonParameterModifier, inClassBody)
        add(JetTokens.FINAL_KEYWORD, inTopLevel, inNonParameterModifier, inClassBody)
        add(JetTokens.OPEN_KEYWORD, inTopLevel, inNonParameterModifier, inClassBody)

        add(JetTokens.INTERNAL_KEYWORD, inTopLevel, inNonParameterModifier, inClassBody, inNonClassBlock)
        add(JetTokens.PRIVATE_KEYWORD, inTopLevel, inNonParameterModifier, inClassBody, inNonClassBlock)
        add(JetTokens.PROTECTED_KEYWORD, inTopLevel, inNonParameterModifier, inClassBody, inNonClassBlock)
        add(JetTokens.PUBLIC_KEYWORD, inTopLevel, inNonParameterModifier, inClassBody, inNonClassBlock)

        add(JetTokens.CLASS_KEYWORD, inTopLevel, inClassBody, inNonClassBlock)
        add(JetTokens.ENUM_KEYWORD, inTopLevel, inClassBody, inNonClassBlock)
        add(JetTokens.FUN_KEYWORD, inTopLevel, inClassBody, inNonClassBlock)
        add(JetTokens.GET_KEYWORD, inTopLevel, inClassBody, inNonClassBlock)
        add(JetTokens.SET_KEYWORD, inTopLevel, inClassBody, inNonClassBlock)
        add(JetTokens.TRAIT_KEYWORD, inTopLevel, inClassBody, inNonClassBlock)
        add(JetTokens.VAL_KEYWORD, inTopLevel, inClassBody, inNonClassBlock)
        add(JetTokens.VAR_KEYWORD, inTopLevel, inClassBody, inNonClassBlock)
        add(JetTokens.TYPE_KEYWORD, inTopLevel, inClassBody, inNonClassBlock)

        add(JetTokens.IMPORT_KEYWORD, inTopLevel)
        add(JetTokens.PACKAGE_KEYWORD, inTopLevel)

        add(JetTokens.OVERRIDE_KEYWORD, inClassBody)

        add(JetTokens.IN_KEYWORD, inNonClassBlock, InTypeParameterFirstChildFilter)

        add(JetTokens.OUT_KEYWORD, InTypeParameterFirstChildFilter)

        add(JetTokens.OBJECT_KEYWORD, inNonClassBlock, AfterClassInClassBodyFilter)

        add(JetTokens.ELSE_KEYWORD, inNonClassBlock, inPropertyBody)
        add(JetTokens.IF_KEYWORD, inNonClassBlock, inPropertyBody)
        add(JetTokens.TRUE_KEYWORD, inNonClassBlock, inPropertyBody)
        add(JetTokens.FALSE_KEYWORD, inNonClassBlock, inPropertyBody)
        add(JetTokens.NULL_KEYWORD, inNonClassBlock, inPropertyBody)
        add(JetTokens.THIS_KEYWORD, inNonClassBlock, inPropertyBody)
        add(JetTokens.WHEN_KEYWORD, inNonClassBlock, inPropertyBody)

        add(JetTokens.AS_KEYWORD, inNonClassBlock)
        add(JetTokens.BREAK_KEYWORD, inNonClassBlock)
        add(JetTokens.BY_KEYWORD, inNonClassBlock)
        add(JetTokens.CATCH_KEYWORD, inNonClassBlock)
        add(JetTokens.CONTINUE_KEYWORD, inNonClassBlock)
        add(JetTokens.DO_KEYWORD, inNonClassBlock)
        add(JetTokens.FINALLY_KEYWORD, inNonClassBlock)
        add(JetTokens.FOR_KEYWORD, inNonClassBlock)
        add(JetTokens.IS_KEYWORD, inNonClassBlock)
        add(JetTokens.RETURN_KEYWORD, inNonClassBlock)
        add(JetTokens.SUPER_KEYWORD, inNonClassBlock)
        add(JetTokens.CAPITALIZED_THIS_KEYWORD, inNonClassBlock)
        add(JetTokens.THROW_KEYWORD, inNonClassBlock)
        add(JetTokens.TRY_KEYWORD, inNonClassBlock)
        add(JetTokens.VARARG_KEYWORD, inNonClassBlock)
        add(JetTokens.WHERE_KEYWORD, inNonClassBlock)
        add(JetTokens.WHILE_KEYWORD, inNonClassBlock)

        val result = ArrayList<Pair<ElementFilter, Collection<String>>>()
        for ((filters, tokens) in filtersToKeywords.entrySet()) {
            val orFilter = OrFilter()
            filters.forEach { filter -> orFilter.addFilter(filter) }
            result.add(orFilter to tokens)
        }

        result
    }

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

    private object InTopFilter : PositionElementFilter() {
        override fun isAcceptable(element : Any?, context : PsiElement?) : Boolean {
            val underFile = PsiTreeUtil.getParentOfType(context, javaClass<JetFile>(), false,
                    javaClass<JetClass>(), javaClass<JetClassBody>(), javaClass<JetBlockExpression>(), javaClass<JetFunction>()) != null

            val notInDeclarationElement = PsiTreeUtil.getParentOfType(
                    context,
                    javaClass<JetParameterList>(), javaClass<JetTypeParameterList>(), javaClass<JetClass>()) == null

            return underFile && notInDeclarationElement
        }
    }

    private object InNonClassBlockFilter : PositionElementFilter() {
        override fun isAcceptable(element : Any?, context : PsiElement?)
                = PsiTreeUtil.getParentOfType(context, javaClass<JetBlockExpression>(), true, javaClass<JetClassBody>()) != null
    }

    private object InTypeParameterFirstChildFilter : PositionElementFilter() {
        override fun isAcceptable(element : Any?, context : PsiElement?) : Boolean {
            val typeParameterElement = PsiTreeUtil.getParentOfType(context, javaClass<JetTypeParameter>(), true)
            return typeParameterElement != null &&
                    context != null &&
                    PsiTreeUtil.isAncestor(typeParameterElement.getFirstChild(), context, false)
        }
    }

    private open class InClassBodyFilter() : PositionElementFilter() {
        override fun isAcceptable(element : Any?, context : PsiElement?) : Boolean {
            return PsiTreeUtil.getParentOfType(
                    context, javaClass<JetClassBody>(), true,
                    javaClass<JetBlockExpression>(),
                    javaClass<JetProperty>(),
                    javaClass<JetParameterList>()) != null
        }
    }

    private object AfterClassInClassBodyFilter : InClassBodyFilter() {
        override fun isAcceptable(element : Any?, context : PsiElement?) : Boolean {
            if (super.isAcceptable(element, context)) {
                fun isLeafAndClass(psiElement: PsiElement?) =
                        psiElement is LeafPsiElement && psiElement.getElementType() == JetTokens.CLASS_KEYWORD

                val ps = context?.getPrevSibling()
                return isLeafAndClass(if (ps is PsiWhiteSpace) ps.getPrevSibling() else ps)
            }

            return false
        }
    }

    private object InPropertyBodyFilter : PositionElementFilter() {
        override fun isAcceptable(element: Any?, context: PsiElement?): Boolean {
            if (element !is PsiElement) return false

            val property = PsiTreeUtil.getParentOfType(context, javaClass<JetProperty>(), false)
            return property != null && isAfterName(property, (element as PsiElement))
        }

        private fun isAfterName(property : JetProperty, element : PsiElement) : Boolean {
            var iterableChild = property.getFirstChild()
            while (iterableChild != null) {
                val child = iterableChild!!

                if (PsiTreeUtil.isAncestor(child, element, false)) {
                    break
                }

                if (child.getNode()?.getElementType() == JetTokens.IDENTIFIER) {
                    return true
                }

                iterableChild = child.getNextSibling()
            }

            return false
        }
    }
}
