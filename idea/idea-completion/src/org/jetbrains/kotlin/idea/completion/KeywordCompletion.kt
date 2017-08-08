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

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.*
import com.intellij.psi.filters.*
import com.intellij.psi.filters.position.LeftNeighbour
import com.intellij.psi.filters.position.PositionElementFilter
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget.*
import org.jetbrains.kotlin.idea.completion.handlers.WithTailInsertHandler
import org.jetbrains.kotlin.idea.completion.handlers.createKeywordConstructLookupElement
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.ModifierCheckerCore
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

open class KeywordLookupObject

object KeywordCompletion {
    private val ALL_KEYWORDS = (KEYWORDS.types + SOFT_KEYWORDS.types)
            .map { it as KtKeywordToken }

    private val KEYWORDS_TO_IGNORE_PREFIX = TokenSet.create(OVERRIDE_KEYWORD /* it's needed to complete overrides that should be work by member name too */)

    private val COMPOUND_KEYWORDS = mapOf<KtKeywordToken, KtKeywordToken>(
            COMPANION_KEYWORD to OBJECT_KEYWORD,
            ENUM_KEYWORD to CLASS_KEYWORD,
            ANNOTATION_KEYWORD to CLASS_KEYWORD,
            SEALED_KEYWORD to CLASS_KEYWORD,
            LATEINIT_KEYWORD to VAR_KEYWORD
    )

    private val KEYWORD_CONSTRUCTS = mapOf<KtKeywordToken, String>(
            IF_KEYWORD to "fun foo() { if (caret)",
            WHILE_KEYWORD to "fun foo() { while(caret)",
            FOR_KEYWORD to "fun foo() { for(caret)",
            TRY_KEYWORD to "fun foo() { try {\ncaret\n}",
            CATCH_KEYWORD to "fun foo() { try {} catch (caret)",
            FINALLY_KEYWORD to "fun foo() { try {\n}\nfinally{\ncaret\n}",
            DO_KEYWORD to "fun foo() { do {\ncaret\n}",
            INIT_KEYWORD to "class C { init {\ncaret\n}",
            CONSTRUCTOR_KEYWORD to "class C { constructor(caret)",
            COMPANION_KEYWORD to "class C { companion object {\ncaret\n}"
    )

    private val NO_SPACE_AFTER = listOf(THIS_KEYWORD,
                                        SUPER_KEYWORD,
                                        NULL_KEYWORD,
                                        TRUE_KEYWORD,
                                        FALSE_KEYWORD,
                                        BREAK_KEYWORD,
                                        CONTINUE_KEYWORD,
                                        ELSE_KEYWORD,
                                        WHEN_KEYWORD,
                                        FILE_KEYWORD,
                                        DYNAMIC_KEYWORD,
                                        GET_KEYWORD,
                                        SET_KEYWORD).map { it.value} + "companion object"

    fun complete(position: PsiElement, prefix: String, isJvmModule: Boolean, consumer: (LookupElement) -> Unit) {
        if (!GENERAL_FILTER.isAcceptable(position, position)) return

        val parserFilter = buildFilter(position)
        for (keywordToken in ALL_KEYWORDS) {
            var keyword = keywordToken.value

            val nextKeyword = COMPOUND_KEYWORDS[keywordToken]
            var applicableAsCompound = false
            if (nextKeyword != null) {
                fun PsiElement.isSpace() = this is PsiWhiteSpace && '\n' !in getText()

                var next = position.nextLeaf { !(it.isSpace() || it.text == "$") }?.text
                if (next != null && next.startsWith("$")) {
                    next = next.substring(1)
                }
                if (next != nextKeyword.value)
                    keyword += " " + nextKeyword.value
                else
                    applicableAsCompound = true
            }

            if (keywordToken == DYNAMIC_KEYWORD && isJvmModule) continue // not supported for JVM

            // we use simple matching by prefix, not prefix matcher from completion
            if (!keyword.startsWith(prefix) && keywordToken !in KEYWORDS_TO_IGNORE_PREFIX) continue

            if (!parserFilter(keywordToken)) continue

            val constructText = KEYWORD_CONSTRUCTS[keywordToken]
            if (constructText != null && !applicableAsCompound) {
                val element = createKeywordConstructLookupElement(position.project, keyword, constructText)
                consumer(element)
            }
            else {
                var element = LookupElementBuilder.create(KeywordLookupObject(), keyword).bold()

                val isUseSiteAnnotationTarget = position.prevLeaf()?.node?.elementType == KtTokens.AT

                val insertHandler = when {
                    isUseSiteAnnotationTarget -> UseSiteAnnotationTargetInsertHandler

                    keyword in NO_SPACE_AFTER -> null

                    else -> SpaceAfterInsertHandler
                }

                element = element.withInsertHandler(insertHandler)

                if (isUseSiteAnnotationTarget) {
                    element = element.withPresentableText(keyword + ":")
                }

                consumer(element)
            }
        }
    }

    private object UseSiteAnnotationTargetInsertHandler : InsertHandler<LookupElement> {
        override fun handleInsert(context: InsertionContext, item: LookupElement) {
            WithTailInsertHandler(":", spaceBefore = false, spaceAfter = false).postHandleInsert(context, item)
        }
    }

    private object SpaceAfterInsertHandler : InsertHandler<LookupElement> {
        override fun handleInsert(context: InsertionContext, item: LookupElement) {
            WithTailInsertHandler.SPACE.postHandleInsert(context, item)
        }
    }

    private val GENERAL_FILTER = NotFilter(OrFilter(
            CommentFilter(),
            ParentFilter(ClassFilter(KtLiteralStringTemplateEntry::class.java)),
            ParentFilter(ClassFilter(KtConstantExpression::class.java)),
            FileFilter(ClassFilter(KtTypeCodeFragment::class.java)),
            LeftNeighbour(TextFilter(".")),
            LeftNeighbour(TextFilter("?."))
    ))

    private class CommentFilter() : ElementFilter {
        override fun isAcceptable(element : Any?, context : PsiElement?)
                = (element is PsiElement) && KtPsiUtil.isInComment(element)

        override fun isClassAcceptable(hintClass: Class<out Any?>)
                = true
    }

    private class ParentFilter(filter : ElementFilter) : PositionElementFilter() {
        init {
            setFilter(filter)
        }

        override fun isAcceptable(element : Any?, context : PsiElement?) : Boolean {
            val parent = (element as? PsiElement)?.parent
            return parent != null && (filter?.isAcceptable(parent, context) ?: true)
        }
    }

    private class FileFilter(filter : ElementFilter) : PositionElementFilter() {
        init {
            setFilter(filter)
        }

        override fun isAcceptable(element : Any?, context : PsiElement?) : Boolean {
            val file = (element as? PsiElement)?.containingFile
            return file != null && (filter?.isAcceptable(file, context) ?: true)
        }
    }

    private fun buildFilter(position: PsiElement): (KtKeywordToken) -> Boolean {
        var parent = position.parent
        var prevParent = position
        while (parent != null) {
            when (parent) {
                is KtBlockExpression -> {
                    var prefixText = "fun foo() { "
                    if (prevParent is KtExpression) {
                        // check that we are right after a try-expression without finally-block or after if-expression without else
                        val prevLeaf = prevParent.prevLeaf { it !is PsiWhiteSpace && it !is PsiComment && it !is PsiErrorElement }
                        if (prevLeaf != null) {
                            val isAfterThen = prevLeaf.goUpWhileIsLastChild().any { it.node.elementType == KtNodeTypes.THEN }

                            var isAfterTry = false
                            var isAfterCatch = false
                            if (prevLeaf.node.elementType == KtTokens.RBRACE) {
                                val blockParent = (prevLeaf.parent as? KtBlockExpression)?.parent
                                when (blockParent) {
                                    is KtTryExpression -> isAfterTry = true
                                    is KtCatchClause -> { isAfterTry = true; isAfterCatch = true }
                                }
                            }

                            if (isAfterThen) {
                                if (isAfterTry) {
                                    prefixText += "if (a)\n"
                                }
                                else {
                                    prefixText += "if (a) {}\n"
                                }
                            }
                            if (isAfterTry) {
                                prefixText += "try {}\n"
                            }
                            if (isAfterCatch) {
                                prefixText += "catch (e: E) {}\n"
                            }
                        }

                        return buildFilterWithContext(prefixText, prevParent, position)
                    }
                    else {
                        val lastExpression = prevParent
                                .siblings(forward = false, withItself = false)
                                .firstIsInstanceOrNull<KtExpression>()
                        if (lastExpression != null) {
                            val contextAfterExpression = lastExpression
                                    .siblings(forward = true, withItself = false)
                                    .takeWhile { it != prevParent }
                                    .joinToString { it.text }
                            return buildFilterWithContext(prefixText + "x" + contextAfterExpression, prevParent, position)
                        }
                    }
                }

                is KtDeclarationWithInitializer -> {
                    val initializer = parent.initializer
                    if (prevParent == initializer) {
                        return buildFilterWithContext("val v = ", initializer!!, position)
                    }
                }

                is KtParameter -> {
                    val default = parent.defaultValue
                    if (prevParent == default) {
                        return buildFilterWithContext("val v = ", default!!, position)
                    }
                }

                is KtDeclaration -> {
                    val scope = parent.parent
                    when (scope) {
                        is KtClassOrObject -> {
                            return if (parent is KtPrimaryConstructor) {
                                buildFilterWithReducedContext("class X ", parent, position)
                            }
                            else {
                                buildFilterWithReducedContext("class X { ", parent, position)
                            }
                        }

                        is KtFile -> return buildFilterWithReducedContext("", parent, position)
                    }
                }
            }


            prevParent = parent
            parent = parent.parent
        }

        return buildFilterWithReducedContext("", null, position)
    }

    private fun computeKeywordApplications(prefixText: String, keyword: KtKeywordToken): Sequence<String> = when (keyword) {
        SUSPEND_KEYWORD -> sequenceOf("suspend () -> Unit>", "suspend X")
        else -> {
            if (prefixText.endsWith("@"))
                sequenceOf(keyword.value + ":X Y.Z")
            else
                sequenceOf(keyword.value + " X")
        }
    }

    private fun buildFilterWithContext(prefixText: String,
                                       contextElement: PsiElement,
                                       position: PsiElement): (KtKeywordToken) -> Boolean {
        val offset = position.getStartOffsetInAncestor(contextElement)
        val truncatedContext = contextElement.text!!.substring(0, offset)
        return buildFilterByText(prefixText + truncatedContext, position)
    }

    private fun buildFilterWithReducedContext(prefixText: String,
                                              contextElement: PsiElement?,
                                              position: PsiElement): (KtKeywordToken) -> Boolean {
        val builder = StringBuilder()
        buildReducedContextBefore(builder, position, contextElement)
        return buildFilterByText(prefixText + builder.toString(), position)
    }


    private fun buildFilesWithKeywordApplication(keywordTokenType: KtKeywordToken, prefixText: String, psiFactory: KtPsiFactory): Sequence<KtFile> {
        return computeKeywordApplications(prefixText, keywordTokenType)
                .map { application -> psiFactory.createFile(prefixText + application) }
    }


    private fun buildFilterByText(prefixText: String, position: PsiElement): (KtKeywordToken) -> Boolean {
        val psiFactory = KtPsiFactory(position.project)
        fun isKeywordCorrectlyApplied(keywordTokenType: KtKeywordToken, file: KtFile): Boolean {
            val elementAt = file.findElementAt(prefixText.length)!!

            when {
                !elementAt.node!!.elementType.matchesKeyword(keywordTokenType) -> return false

                elementAt.getNonStrictParentOfType<PsiErrorElement>() != null -> return false

                isErrorElementBefore(elementAt) -> return false

                !isSupportedAtLanguageLevel(keywordTokenType, position) -> return false

                keywordTokenType !is KtModifierKeywordToken -> return true

                else -> {
                    if (elementAt.parent !is KtModifierList) return true
                    val container = elementAt.parent.parent
                    val possibleTargets = when (container) {
                        is KtParameter -> {
                            if (container.ownerFunction is KtPrimaryConstructor)
                                listOf(VALUE_PARAMETER, MEMBER_PROPERTY)
                            else
                                listOf(VALUE_PARAMETER)
                        }

                        is KtTypeParameter -> listOf(TYPE_PARAMETER)

                        is KtEnumEntry -> listOf(ENUM_ENTRY)

                        is KtClassBody -> listOf(CLASS_ONLY, INTERFACE, OBJECT, ENUM_CLASS, ANNOTATION_CLASS, MEMBER_FUNCTION, MEMBER_PROPERTY, FUNCTION, PROPERTY)

                        is KtFile -> listOf(CLASS_ONLY, INTERFACE, OBJECT, ENUM_CLASS, ANNOTATION_CLASS, TOP_LEVEL_FUNCTION, TOP_LEVEL_PROPERTY, FUNCTION, PROPERTY)

                        else -> null
                    }
                    val modifierTargets = ModifierCheckerCore.possibleTargetMap[keywordTokenType]
                    if (modifierTargets != null && possibleTargets != null && possibleTargets.none { it in modifierTargets }) return false

                    val ownerDeclaration = container?.getParentOfType<KtDeclaration>(strict = true)
                    val parentTarget = when (ownerDeclaration) {
                        null -> KotlinTarget.FILE

                        is KtClass -> {
                            when {
                                ownerDeclaration.isInterface() -> KotlinTarget.INTERFACE
                                ownerDeclaration.isEnum() -> KotlinTarget.ENUM_CLASS
                                ownerDeclaration.isAnnotation() -> KotlinTarget.ANNOTATION_CLASS
                                else -> KotlinTarget.CLASS_ONLY
                            }
                        }

                        is KtObjectDeclaration -> if (ownerDeclaration.isObjectLiteral()) KotlinTarget.OBJECT_LITERAL else KotlinTarget.OBJECT

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

        return fun (keywordTokenType): Boolean {
            val files = buildFilesWithKeywordApplication(keywordTokenType, prefixText, psiFactory)
            return files.any { file -> isKeywordCorrectlyApplied(keywordTokenType, file); }
        }
    }

    private fun isErrorElementBefore(token: PsiElement): Boolean {
        for (leaf in token.prevLeafs) {
            if (leaf is PsiWhiteSpace || leaf is PsiComment) continue
            if (leaf.parentsWithSelf.any { it is PsiErrorElement } ) return true
            if (leaf.textLength != 0) break
        }
        return false
    }

    private fun IElementType.matchesKeyword(keywordType: KtKeywordToken): Boolean {
        return when(this) {
            keywordType -> true
            NOT_IN -> keywordType == IN_KEYWORD
            NOT_IS -> keywordType == IS_KEYWORD
            else -> false
        }
    }

    private fun isSupportedAtLanguageLevel(keyword: KtKeywordToken, position: PsiElement): Boolean {
        val languageVersionSettings = ModuleUtilCore.findModuleForPsiElement(position)?.languageVersionSettings
                                      ?: LanguageVersionSettingsImpl.DEFAULT
        val feature = when (keyword) {
            KtTokens.TYPE_ALIAS_KEYWORD -> LanguageFeature.TypeAliases
            KtTokens.HEADER_KEYWORD, KtTokens.IMPL_KEYWORD -> LanguageFeature.MultiPlatformProjects
            KtTokens.SUSPEND_KEYWORD -> LanguageFeature.Coroutines
            else -> return true
        }
        return languageVersionSettings.supportsFeature(feature)
    }

    // builds text within scope (or from the start of the file) before position element excluding almost all declarations
    private fun buildReducedContextBefore(builder: StringBuilder, position: PsiElement, scope: PsiElement?) {
        if (position == scope) return
        val parent = position.parent ?: return

        buildReducedContextBefore(builder, parent, scope)

        val prevDeclaration = position.siblings(forward = false, withItself = false).firstOrNull { it is KtDeclaration }

        var child = parent.firstChild
        while (child != position) {
            if (child is KtDeclaration) {
                if (child == prevDeclaration) {
                    builder.appendReducedText(child)
                }
            }
            else {
                builder.append(child!!.text)
            }

            child = child.nextSibling
        }
    }

    private fun StringBuilder.appendReducedText(element: PsiElement) {
        var child = element.firstChild
        if (child == null) {
            append(element.text!!)
        }
        else {
            while (child != null) {
                when (child) {
                    is KtBlockExpression, is KtClassBody -> append("{}")
                    else -> appendReducedText(child)
                }

                child = child.nextSibling
            }
        }
    }

    private fun PsiElement.getStartOffsetInAncestor(ancestor: PsiElement): Int {
        if (ancestor == this) return 0
        return parent!!.getStartOffsetInAncestor(ancestor) + startOffsetInParent
    }

    private fun PsiElement.goUpWhileIsLastChild(): Sequence<PsiElement> {
        return generateSequence(this) {
            if (it is PsiFile)
                null
            else if (it != it.parent.lastChild)
                null
            else
                it.parent
        }
    }
}
