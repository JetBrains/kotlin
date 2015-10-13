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

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.filters.*
import com.intellij.psi.filters.position.LeftNeighbour
import com.intellij.psi.filters.position.PositionElementFilter
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget.*
import org.jetbrains.kotlin.idea.completion.handlers.KotlinFunctionInsertHandler
import org.jetbrains.kotlin.idea.completion.handlers.KotlinKeywordInsertHandler
import org.jetbrains.kotlin.lexer.JetKeywordToken
import org.jetbrains.kotlin.lexer.JetModifierKeywordToken
import org.jetbrains.kotlin.lexer.JetTokens.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.ModifierCheckerCore

open class KeywordLookupObject

object KeywordCompletion {
    private val NON_ACTUAL_KEYWORDS = setOf(CAPITALIZED_THIS_KEYWORD,
                                            TYPE_ALIAS_KEYWORD)
    private val ALL_KEYWORDS = (KEYWORDS.getTypes() + SOFT_KEYWORDS.getTypes())
            .filter { it !in NON_ACTUAL_KEYWORDS }
            .map { it as JetKeywordToken }

    private val DEFAULT_DUMMY_POSTFIX = " X"
    private val KEYWORD_TO_DUMMY_POSTFIX = mapOf(FILE_KEYWORD to ":")

    private val KEYWORDS_TO_IGNORE_PREFIX = TokenSet.create(OVERRIDE_KEYWORD /* it's needed to complete overrides that should be work by member name too */)

    public fun complete(position: PsiElement, prefix: String, consumer: (LookupElement) -> Unit) {
        if (!GENERAL_FILTER.isAcceptable(position, position)) return

        val parserFilter = buildFilter(position)
        for (keywordToken in ALL_KEYWORDS) {
            var keyword = keywordToken.getValue()

            if (keyword == COMPANION_KEYWORD.getValue()) { // complete "companion object" instead of simply "companion" unless we are before "object" already
                fun PsiElement.isSpace() = this is PsiWhiteSpace && '\n' !in getText()

                var next = position.nextLeaf { !(it.isSpace() || it.getText() == "$") }?.getText()
                if (next != null && next.startsWith("$")) {
                    next = next.substring(1)
                }
                if (next != OBJECT_KEYWORD.getValue()) {
                    keyword += " " + OBJECT_KEYWORD.getValue()
                }
            }

            // we use simple matching by prefix, not prefix matcher from completion
            if (!keyword.startsWith(prefix) && keywordToken !in KEYWORDS_TO_IGNORE_PREFIX) continue

            if (!parserFilter(keywordToken)) continue

            val element = LookupElementBuilder.create(KeywordLookupObject(), keyword)
                    .bold()
                    .withInsertHandler(if (keywordToken !in FUNCTION_KEYWORDS)
                                           KotlinKeywordInsertHandler
                                       else
                                           KotlinFunctionInsertHandler.Normal(inputTypeArguments = false, inputValueArguments = false))
            consumer(element)
        }
    }

    private val FUNCTION_KEYWORDS = listOf(GET_KEYWORD, SET_KEYWORD, CONSTRUCTOR_KEYWORD)

    private val GENERAL_FILTER = NotFilter(OrFilter(
            CommentFilter(),
            ParentFilter(ClassFilter(javaClass<JetLiteralStringTemplateEntry>())),
            ParentFilter(ClassFilter(javaClass<JetConstantExpression>())),
            LeftNeighbour(TextFilter(".")),
            LeftNeighbour(TextFilter("?."))
    ))

    private class CommentFilter() : ElementFilter {
        override fun isAcceptable(element : Any?, context : PsiElement?)
                = (element is PsiElement) && JetPsiUtil.isInComment(element)

        override fun isClassAcceptable(hintClass: Class<out Any?>)
                = true
    }

    private class ParentFilter(filter : ElementFilter) : PositionElementFilter() {
        init {
            setFilter(filter)
        }

        override fun isAcceptable(element : Any?, context : PsiElement?) : Boolean {
            val parent = (element as? PsiElement)?.getParent()
            return parent != null && (getFilter()?.isAcceptable(parent, context) ?: true)
        }
    }

    private fun buildFilter(position: PsiElement): (JetKeywordToken) -> Boolean {
        var parent = position.getParent()
        var prevParent = position
        while (parent != null) {
            when (parent) {
                is JetBlockExpression -> {
                    return buildFilterWithContext("fun foo() { ", prevParent, position)
                }

                is JetWithExpressionInitializer -> {
                    val initializer = parent.getInitializer()
                    if (prevParent == initializer) {
                        return buildFilterWithContext("val v = ", initializer!!, position)
                    }
                }

                is JetParameter -> {
                    val default = parent.getDefaultValue()
                    if (prevParent == default) {
                        return buildFilterWithContext("val v = ", default!!, position)
                    }
                }
            }

            if (parent is JetDeclaration) {
                val scope = parent.parent
                when (scope) {
                    is JetClassOrObject -> {
                        if (parent is JetPrimaryConstructor) {
                            return buildFilterWithReducedContext("class X ", parent, position)
                        }
                        else {
                            return buildFilterWithReducedContext("class X { ", parent, position)
                        }
                    }

                    is JetFile -> return buildFilterWithReducedContext("", parent, position)
                }
            }

            prevParent = parent
            parent = parent.parent
        }

        return buildFilterWithReducedContext("", null, position)
    }

    private fun buildFilterWithContext(prefixText: String,
                                       contextElement: PsiElement,
                                       position: PsiElement): (JetKeywordToken) -> Boolean {
        val offset = position.getStartOffsetInAncestor(contextElement)
        val truncatedContext = contextElement.getText()!!.substring(0, offset)
        return buildFilterByText(prefixText + truncatedContext, contextElement.getProject())
    }

    private fun buildFilterWithReducedContext(prefixText: String,
                                              contextElement: PsiElement?,
                                              position: PsiElement): (JetKeywordToken) -> Boolean {
        val builder = StringBuilder()
        buildReducedContextBefore(builder, position, contextElement)
        return buildFilterByText(prefixText + builder.toString(), position.getProject())
    }


    private fun buildFilterByText(prefixText: String, project: Project): (JetKeywordToken) -> Boolean {
        val psiFactory = JetPsiFactory(project)
        return fun (keywordTokenType): Boolean {
            val postfix = KEYWORD_TO_DUMMY_POSTFIX[keywordTokenType] ?: DEFAULT_DUMMY_POSTFIX
            val file = psiFactory.createFile(prefixText + keywordTokenType.getValue() + postfix)
            val elementAt = file.findElementAt(prefixText.length())!!

            when {
                !elementAt.getNode()!!.getElementType().matchesKeyword(keywordTokenType) -> return false

                elementAt.getNonStrictParentOfType<PsiErrorElement>() != null -> return false

                elementAt.prevLeaf { it !is PsiWhiteSpace && it !is PsiComment }?.parentsWithSelf?.any { it is PsiErrorElement } ?: false -> return false

                keywordTokenType !is JetModifierKeywordToken -> return true

                else -> {
                    if (elementAt.parent !is JetModifierList) return true
                    val container = elementAt.parent.parent
                    val possibleTargets = when (container) {
                        is JetParameter -> {
                            if (container.ownerFunction is JetPrimaryConstructor)
                                listOf(VALUE_PARAMETER, MEMBER_PROPERTY)
                            else
                                listOf(VALUE_PARAMETER)
                        }

                        is JetTypeParameter -> listOf(TYPE_PARAMETER)

                        is JetEnumEntry -> listOf(ENUM_ENTRY)

                        is JetClassBody -> listOf(CLASS_ONLY, INTERFACE, OBJECT, ENUM_CLASS, ANNOTATION_CLASS, INNER_CLASS, MEMBER_FUNCTION, MEMBER_PROPERTY, FUNCTION, PROPERTY)

                        is JetFile -> listOf(CLASS_ONLY, INTERFACE, OBJECT, ENUM_CLASS, ANNOTATION_CLASS, TOP_LEVEL_FUNCTION, TOP_LEVEL_PROPERTY, FUNCTION, PROPERTY)

                        else -> null
                    }
                    val modifierTargets = ModifierCheckerCore.possibleTargetMap[keywordTokenType]
                    if (modifierTargets != null && possibleTargets != null && possibleTargets.none { it in modifierTargets }) return false

                    val ownerDeclaration = container?.getParentOfType<JetDeclaration>(strict = true)
                    val parentTarget = when (ownerDeclaration) {
                        null -> KotlinTarget.FILE

                        is JetClass -> {
                            when {
                                ownerDeclaration.isInterface() -> KotlinTarget.INTERFACE
                                ownerDeclaration.isEnum() -> KotlinTarget.ENUM_CLASS
                                ownerDeclaration.isAnnotation() -> KotlinTarget.ANNOTATION_CLASS
                                ownerDeclaration.isInner() -> KotlinTarget.INNER_CLASS
                                else -> KotlinTarget.CLASS_ONLY
                            }
                        }

                        is JetObjectDeclaration -> if (ownerDeclaration.isObjectLiteral()) KotlinTarget.OBJECT_LITERAL else KotlinTarget.OBJECT

                        else -> return true
                    }

                    val modifierParents = ModifierCheckerCore.possibleParentTargetMap[keywordTokenType]
                    if (modifierParents != null && parentTarget !in modifierParents) return false

                    val deprecatedParents = ModifierCheckerCore.deprecatedParentTargetMap[keywordTokenType]
                    if (deprecatedParents != null && parentTarget in deprecatedParents) return false

                    return true
                }
            }
        }
    }

    private fun IElementType.matchesKeyword(keywordType: JetKeywordToken): Boolean {
        return when(this) {
            keywordType -> true
            NOT_IN -> keywordType == IN_KEYWORD
            NOT_IS -> keywordType == IS_KEYWORD
            else -> false
        }
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
                    builder.appendReducedText(child)
                }
            }
            else {
                builder.append(child!!.getText())
            }

            child = child.getNextSibling()
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
        if (ancestor == this) return 0
        return getParent()!!.getStartOffsetInAncestor(ancestor) + getStartOffsetInParent()
    }
}
