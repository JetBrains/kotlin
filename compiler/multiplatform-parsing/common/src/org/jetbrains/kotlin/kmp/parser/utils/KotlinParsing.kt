/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.parser.utils

import com.intellij.platform.syntax.SyntaxElementType
import com.intellij.platform.syntax.SyntaxElementTypeSet
import com.intellij.platform.syntax.emptySyntaxElementTypeSet
import com.intellij.platform.syntax.parser.SyntaxTreeBuilder
import com.intellij.platform.syntax.parser.WhitespacesBinders
import com.intellij.platform.syntax.syntaxElementTypeSetOf
import org.jetbrains.annotations.Contract
import org.jetbrains.kotlin.kmp.lexer.KtTokens
import org.jetbrains.kotlin.kmp.lexer.KtTokens.BREAK_KEYWORD
import org.jetbrains.kotlin.kmp.lexer.KtTokens.CONTINUE_KEYWORD
import org.jetbrains.kotlin.kmp.lexer.KtTokens.CONTRACT_MODIFIER
import org.jetbrains.kotlin.kmp.lexer.KtTokens.DO_KEYWORD
import org.jetbrains.kotlin.kmp.lexer.KtTokens.FALSE_KEYWORD
import org.jetbrains.kotlin.kmp.lexer.KtTokens.FOR_KEYWORD
import org.jetbrains.kotlin.kmp.lexer.KtTokens.FUN_MODIFIER
import org.jetbrains.kotlin.kmp.lexer.KtTokens.IF_KEYWORD
import org.jetbrains.kotlin.kmp.lexer.KtTokens.NULL_KEYWORD
import org.jetbrains.kotlin.kmp.lexer.KtTokens.OBJECT_KEYWORD
import org.jetbrains.kotlin.kmp.lexer.KtTokens.RETURN_KEYWORD
import org.jetbrains.kotlin.kmp.lexer.KtTokens.SUPER_KEYWORD
import org.jetbrains.kotlin.kmp.lexer.KtTokens.THIS_KEYWORD
import org.jetbrains.kotlin.kmp.lexer.KtTokens.THROW_KEYWORD
import org.jetbrains.kotlin.kmp.lexer.KtTokens.TRUE_KEYWORD
import org.jetbrains.kotlin.kmp.lexer.KtTokens.TRY_KEYWORD
import org.jetbrains.kotlin.kmp.lexer.KtTokens.WHEN_KEYWORD
import org.jetbrains.kotlin.kmp.lexer.KtTokens.WHILE_KEYWORD
import org.jetbrains.kotlin.kmp.parser.KtNodeTypes

internal class KotlinParsing private constructor(builder: SemanticWhitespaceAwareSyntaxBuilder, isTopLevel: Boolean, isLazy: Boolean) :
    AbstractKotlinParsing(builder, isLazy) {
    companion object {
        private val GT_COMMA_COLON_SET = syntaxElementTypeSetOf(KtTokens.GT, KtTokens.COMMA, KtTokens.COLON)

        private val TOP_LEVEL_DECLARATION_FIRST = syntaxElementTypeSetOf(
            KtTokens.TYPE_ALIAS_KEYWORD, KtTokens.INTERFACE_KEYWORD, KtTokens.CLASS_KEYWORD, KtTokens.OBJECT_KEYWORD,
            KtTokens.FUN_MODIFIER, KtTokens.VAL_KEYWORD, KtTokens.VAR_KEYWORD, KtTokens.PACKAGE_KEYWORD
        )
        private val TOP_LEVEL_DECLARATION_FIRST_SEMICOLON_SET =
            TOP_LEVEL_DECLARATION_FIRST + syntaxElementTypeSetOf(KtTokens.SEMICOLON)
        private val LT_EQ_SEMICOLON_TOP_LEVEL_DECLARATION_FIRST_SET =
            syntaxElementTypeSetOf(KtTokens.LT, KtTokens.EQ, KtTokens.SEMICOLON) + TOP_LEVEL_DECLARATION_FIRST
        private val DECLARATION_FIRST =
            TOP_LEVEL_DECLARATION_FIRST +
                    syntaxElementTypeSetOf(KtTokens.INIT_KEYWORD, KtTokens.GET_KEYWORD, KtTokens.SET_KEYWORD, KtTokens.CONSTRUCTOR_KEYWORD)

        private val CLASS_NAME_RECOVERY_SET =
            syntaxElementTypeSetOf(KtTokens.LT, KtTokens.LPAR, KtTokens.COLON, KtTokens.LBRACE) +
                    TOP_LEVEL_DECLARATION_FIRST

        private val TYPE_PARAMETER_GT_RECOVERY_SET =
            syntaxElementTypeSetOf(KtTokens.WHERE_KEYWORD, KtTokens.LPAR, KtTokens.COLON, KtTokens.LBRACE, KtTokens.GT)
        val PARAMETER_NAME_RECOVERY_SET =
            syntaxElementTypeSetOf(KtTokens.COLON, KtTokens.EQ, KtTokens.COMMA, KtTokens.RPAR, KtTokens.VAL_KEYWORD, KtTokens.VAR_KEYWORD)
        private val PACKAGE_NAME_RECOVERY_SET = syntaxElementTypeSetOf(KtTokens.DOT, KtTokens.EOL_OR_SEMICOLON)
        private val IMPORT_RECOVERY_SET = syntaxElementTypeSetOf(KtTokens.AS_KEYWORD, KtTokens.DOT, KtTokens.EOL_OR_SEMICOLON)
        private val TYPE_REF_FIRST =
            syntaxElementTypeSetOf(KtTokens.LBRACKET, KtTokens.IDENTIFIER, KtTokens.LPAR, KtTokens.HASH, KtTokens.DYNAMIC_KEYWORD)
        private val LBRACE_RBRACE_TYPE_REF_FIRST_SET = syntaxElementTypeSetOf(KtTokens.LBRACE, KtTokens.RBRACE) +
                TYPE_REF_FIRST
        private val COLON_COMMA_LBRACE_RBRACE_TYPE_REF_FIRST_SET =
            syntaxElementTypeSetOf(KtTokens.COLON, KtTokens.COMMA, KtTokens.LBRACE, KtTokens.RBRACE) + TYPE_REF_FIRST
        private val RECEIVER_TYPE_TERMINATORS = syntaxElementTypeSetOf(KtTokens.DOT, KtTokens.SAFE_ACCESS)

        private val MODIFIER_WITHOUT_FUN = KtTokens.MODIFIERS - KtTokens.FUN_MODIFIER
        private val VALUE_PARAMETER_FIRST =
            syntaxElementTypeSetOf(KtTokens.IDENTIFIER, KtTokens.LBRACKET, KtTokens.VAL_KEYWORD, KtTokens.VAR_KEYWORD) +
                    MODIFIER_WITHOUT_FUN
        private val LAMBDA_VALUE_PARAMETER_FIRST =
            syntaxElementTypeSetOf(KtTokens.IDENTIFIER, KtTokens.LBRACKET) +
                    MODIFIER_WITHOUT_FUN

        private val SOFT_KEYWORDS_AT_MEMBER_START = syntaxElementTypeSetOf(KtTokens.CONSTRUCTOR_KEYWORD, KtTokens.INIT_KEYWORD)
        private val ANNOTATION_TARGETS = syntaxElementTypeSetOf(
            KtTokens.ALL_KEYWORD,
            KtTokens.FILE_KEYWORD,
            KtTokens.FIELD_KEYWORD,
            KtTokens.GET_KEYWORD,
            KtTokens.SET_KEYWORD,
            KtTokens.PROPERTY_KEYWORD,
            KtTokens.RECEIVER_KEYWORD,
            KtTokens.PARAM_KEYWORD,
            KtTokens.SETPARAM_KEYWORD,
            KtTokens.DELEGATE_KEYWORD
        )
        private val ANNOTATION_TARGET_ERROR_MESSAGES: Map<SyntaxElementType, String> by lazy(LazyThreadSafetyMode.PUBLICATION) {
            (ANNOTATION_TARGETS + KtTokens.FILE_KEYWORD).associateWith { """Expecting "$it${KtTokens.COLON}" prefix for $it annotations""" }
        }
        private val BLOCK_DOC_COMMENT_SET = syntaxElementTypeSetOf(KtTokens.BLOCK_COMMENT, KtTokens.DOC_COMMENT)
        private val SEMICOLON_SET = syntaxElementTypeSetOf(KtTokens.SEMICOLON)
        private val COMMA_COLON_GT_SET = syntaxElementTypeSetOf(KtTokens.COMMA, KtTokens.COLON, KtTokens.GT)
        private val IDENTIFIER_RBRACKET_LBRACKET_SET = syntaxElementTypeSetOf(KtTokens.IDENTIFIER, KtTokens.RBRACKET, KtTokens.LBRACKET)
        private val LBRACE_RBRACE_SET = syntaxElementTypeSetOf(KtTokens.LBRACE, KtTokens.RBRACE)
        private val COMMA_SEMICOLON_RBRACE_SET = syntaxElementTypeSetOf(KtTokens.COMMA, KtTokens.SEMICOLON, KtTokens.RBRACE)
        private val VALUE_ARGS_RECOVERY_SET =
            syntaxElementTypeSetOf(KtTokens.LBRACE, KtTokens.SEMICOLON, KtTokens.RPAR, KtTokens.EOL_OR_SEMICOLON, KtTokens.RBRACE)
        private val PROPERTY_NAME_FOLLOW_SET = syntaxElementTypeSetOf(
            KtTokens.COLON,
            KtTokens.EQ,
            KtTokens.LBRACE,
            KtTokens.RBRACE,
            KtTokens.SEMICOLON,
            KtTokens.VAL_KEYWORD,
            KtTokens.VAR_KEYWORD,
            KtTokens.FUN_MODIFIER,
            KtTokens.CLASS_KEYWORD
        )
        private val DESTRUCTURING_PROPERTY_NAME_FOLLOW_SET = PROPERTY_NAME_FOLLOW_SET - KtTokens.VAL_VAR
        private val PROPERTY_NAME_FOLLOW_MULTI_DECLARATION_RECOVERY_SET =
            PROPERTY_NAME_FOLLOW_SET + PARAMETER_NAME_RECOVERY_SET
        private val PROPERTY_NAME_FOLLOW_FUNCTION_OR_PROPERTY_RECOVERY_SET =
            PROPERTY_NAME_FOLLOW_SET + LBRACE_RBRACE_SET + TOP_LEVEL_DECLARATION_FIRST
        private val IDENTIFIER_EQ_COLON_SEMICOLON_SET =
            syntaxElementTypeSetOf(KtTokens.IDENTIFIER, KtTokens.EQ, KtTokens.COLON, KtTokens.SEMICOLON)
        private val COMMA_RPAR_COLON_EQ_SET = syntaxElementTypeSetOf(KtTokens.COMMA, KtTokens.RPAR, KtTokens.COLON, KtTokens.EQ)
        private val ACCESSOR_FIRST_OR_PROPERTY_END =
            KtTokens.MODIFIERS +
                    syntaxElementTypeSetOf(
                        KtTokens.AT,
                        KtTokens.GET_KEYWORD,
                        KtTokens.SET_KEYWORD,
                        KtTokens.FIELD_KEYWORD,
                        KtTokens.EOL_OR_SEMICOLON,
                        KtTokens.RBRACE
                    )
        private val RPAR_IDENTIFIER_COLON_LBRACE_EQ_SET =
            syntaxElementTypeSetOf(KtTokens.RPAR, KtTokens.IDENTIFIER, KtTokens.COLON, KtTokens.LBRACE, KtTokens.EQ)
        private val COMMA_COLON_RPAR_SET = syntaxElementTypeSetOf(KtTokens.COMMA, KtTokens.COLON, KtTokens.RPAR)
        private val RPAR_COLON_LBRACE_EQ_SET = syntaxElementTypeSetOf(KtTokens.RPAR, KtTokens.COLON, KtTokens.LBRACE, KtTokens.EQ)
        private val LBRACKET_LBRACE_RBRACE_LPAR_SET =
            syntaxElementTypeSetOf(KtTokens.LBRACKET, KtTokens.LBRACE, KtTokens.RBRACE, KtTokens.LPAR)
        private val FUNCTION_NAME_FOLLOW_SET =
            syntaxElementTypeSetOf(KtTokens.LT, KtTokens.LPAR, KtTokens.RPAR, KtTokens.COLON, KtTokens.EQ)
        private val FUNCTION_NAME_RECOVERY_SET =
            syntaxElementTypeSetOf(KtTokens.LT, KtTokens.LPAR, KtTokens.RPAR, KtTokens.COLON, KtTokens.EQ) +
                    LBRACE_RBRACE_SET +
                    TOP_LEVEL_DECLARATION_FIRST
        private val VALUE_PARAMETERS_FOLLOW_SET =
            syntaxElementTypeSetOf(KtTokens.EQ, KtTokens.LBRACE, KtTokens.RBRACE, KtTokens.SEMICOLON, KtTokens.RPAR)
        private val CONTEXT_PARAMETERS_FOLLOW_SET = syntaxElementTypeSetOf(
            KtTokens.CLASS_KEYWORD,
            KtTokens.OBJECT_KEYWORD,
            KtTokens.FUN_MODIFIER,
            KtTokens.VAL_KEYWORD,
            KtTokens.VAR_KEYWORD
        )
        private val LPAR_VALUE_PARAMETERS_FOLLOW_SET = syntaxElementTypeSetOf(KtTokens.LPAR) + VALUE_PARAMETERS_FOLLOW_SET
        private val LPAR_LBRACE_COLON_CONSTRUCTOR_KEYWORD_SET =
            syntaxElementTypeSetOf(KtTokens.LPAR, KtTokens.LBRACE, KtTokens.COLON, KtTokens.CONSTRUCTOR_KEYWORD)
        private val definitelyOutOfReceiverSet =
            syntaxElementTypeSetOf(KtTokens.EQ, KtTokens.COLON, KtTokens.LBRACE, KtTokens.RBRACE, KtTokens.BY_KEYWORD) +
                    TOP_LEVEL_DECLARATION_FIRST
        private val EOL_OR_SEMICOLON_RBRACE_SET = syntaxElementTypeSetOf(KtTokens.EOL_OR_SEMICOLON, KtTokens.RBRACE)
        private val CLASS_INTERFACE_SET = syntaxElementTypeSetOf(KtTokens.CLASS_KEYWORD, KtTokens.INTERFACE_KEYWORD)

        fun createForTopLevel(builder: SemanticWhitespaceAwareSyntaxBuilder): KotlinParsing {
            return KotlinParsing(builder, isTopLevel = true, isLazy = true)
        }

        fun createForTopLevelNonLazy(builder: SemanticWhitespaceAwareSyntaxBuilder): KotlinParsing {
            return KotlinParsing(builder, isTopLevel = true, isLazy = false)
        }

        private fun createForByClause(builder: SemanticWhitespaceAwareSyntaxBuilder, isLazy: Boolean): KotlinParsing {
            return KotlinParsing(SemanticWhitespaceAwareSyntaxBuilderForByClause(builder), false, isLazy)
        }

        val EXPRESSION_FIRST: SyntaxElementTypeSet = syntaxElementTypeSetOf( // Prefix
            KtTokens.MINUS, KtTokens.PLUS, KtTokens.MINUSMINUS, KtTokens.PLUSPLUS,
            KtTokens.EXCL, KtTokens.EXCLEXCL,  // Joining complex tokens makes it necessary to put EXCLEXCL here
            // Atomic

            KtTokens.COLONCOLON,  // callable reference

            KtTokens.LPAR,  // parenthesized
            // literal constant

            TRUE_KEYWORD, FALSE_KEYWORD,
            KtTokens.INTERPOLATION_PREFIX, KtTokens.OPEN_QUOTE,
            KtTokens.INTEGER_LITERAL, KtTokens.CHARACTER_LITERAL, KtTokens.FLOAT_LITERAL,
            NULL_KEYWORD,

            KtTokens.LBRACE,  // functionLiteral
            FUN_MODIFIER,  // expression function

            THIS_KEYWORD,  // this
            SUPER_KEYWORD,  // super

            IF_KEYWORD,  // if
            WHEN_KEYWORD,  // when
            TRY_KEYWORD,  // try
            OBJECT_KEYWORD,  // object
            // jump

            THROW_KEYWORD,
            RETURN_KEYWORD,
            CONTINUE_KEYWORD,
            BREAK_KEYWORD,  // loop

            FOR_KEYWORD,
            WHILE_KEYWORD,
            DO_KEYWORD,

            KtTokens.IDENTIFIER,  // SimpleName

            KtTokens.AT,  // Just for better recovery and maybe for annotations

            KtTokens.LBRACKET // Collection literal expression
        )

        val EXPRESSION_FOLLOW: SyntaxElementTypeSet = syntaxElementTypeSetOf(
            KtTokens.EOL_OR_SEMICOLON, KtTokens.ARROW, KtTokens.COMMA, KtTokens.RBRACE, KtTokens.RPAR, KtTokens.RBRACKET
        )

        private val USER_TYPE_NAME_RECOVERY_SET: SyntaxElementTypeSet =
            EXPRESSION_FIRST + EXPRESSION_FOLLOW + DECLARATION_FIRST

        private val NO_MODIFIER_BEFORE_FOR_VALUE_PARAMETER =
            syntaxElementTypeSetOf(KtTokens.COMMA, KtTokens.COLON, KtTokens.EQ, KtTokens.RPAR)

        private val LAST_DOT_AFTER_RECEIVER_LPAR_PATTERN_SET = syntaxElementTypeSetOf(KtTokens.QUEST, KtTokens.LPAR, KtTokens.RPAR)

        private val LAST_DOT_AFTER_RECEIVER_NOT_LPAR_PATTERN_SET =
            syntaxElementTypeSetOf(KtTokens.LT, KtTokens.DOT, KtTokens.SAFE_ACCESS, KtTokens.QUEST)

        private val ACCESSOR_BODY_EXPECTED_RECOVERY_SET by lazy(LazyThreadSafetyMode.PUBLICATION) {
            ACCESSOR_FIRST_OR_PROPERTY_END + syntaxElementTypeSetOf(KtTokens.LBRACE, KtTokens.LPAR, KtTokens.EQ)
        }

        private val SECONDARY_CONSTRUCTOR_RECOVERY_SET by lazy(LazyThreadSafetyMode.PUBLICATION) {
            VALUE_ARGS_RECOVERY_SET + syntaxElementTypeSetOf(KtTokens.COLON)
        }

        private val PROPERTY_GETTER_OR_SETTER_EXPECTED_RECOVERY_SET by lazy(LazyThreadSafetyMode.PUBLICATION) {
            DECLARATION_FIRST + syntaxElementTypeSetOf(KtTokens.EOL_OR_SEMICOLON, KtTokens.LBRACE, KtTokens.RBRACE)
        }

        private val PROPERTY_COMPONENT_RECOVERY_SET by lazy(LazyThreadSafetyMode.PUBLICATION) {
            syntaxElementTypeSetOf(
                KtTokens.RPAR,
                KtTokens.COLON,
                KtTokens.LBRACE,
                KtTokens.RBRACE,
                KtTokens.EQ,
                KtTokens.EOL_OR_SEMICOLON
            )
        }

        private val TYPE_REF_CONTENTS_RECOVERY_SET by lazy(LazyThreadSafetyMode.PUBLICATION) {
            TOP_LEVEL_DECLARATION_FIRST +
                    syntaxElementTypeSetOf(
                        KtTokens.EQ,
                        KtTokens.COMMA,
                        KtTokens.GT,
                        KtTokens.RBRACKET,
                        KtTokens.DOT,
                        KtTokens.RPAR,
                        KtTokens.RBRACE,
                        KtTokens.LBRACE,
                        KtTokens.SEMICOLON
                    )
        }

        private val USER_TYPE_RECOVERY_SET by lazy(LazyThreadSafetyMode.PUBLICATION) {
            syntaxElementTypeSetOf(KtTokens.IDENTIFIER, KtTokens.LBRACE, KtTokens.RBRACE)
        }
    }

    private val expressionParsing: KotlinExpressionParsing = if (isTopLevel)
        KotlinExpressionParsing(builder, this, isLazy)
    else
        object : KotlinExpressionParsing(builder, this@KotlinParsing, isLazy) {
            override fun parseCallWithClosure(): Boolean {
                if ((builder as SemanticWhitespaceAwareSyntaxBuilderForByClause).stackSize > 0) {
                    return super.parseCallWithClosure()
                }
                return false
            }

            override fun create(builder: SemanticWhitespaceAwareSyntaxBuilder): KotlinParsing {
                return createForByClause(builder, isLazy)
            }
        }

    private val lastDotAfterReceiverLParPattern = FirstBefore(
        AtSet(RECEIVER_TYPE_TERMINATORS),
        object : AbstractTokenStreamPredicate() {
            override fun matching(topLevel: Boolean): Boolean {
                if (topLevel && atSetWithRemap(definitelyOutOfReceiverSet)) {
                    return true
                }
                return topLevel && !atSet(LAST_DOT_AFTER_RECEIVER_LPAR_PATTERN_SET)
            }
        }
    )

    private val lastDotAfterReceiverNotLParPattern = LastBefore(
        AtSet(RECEIVER_TYPE_TERMINATORS),
        object : AbstractTokenStreamPredicate() {
            override fun matching(topLevel: Boolean): Boolean {
                if (topLevel && (atSetWithRemap(definitelyOutOfReceiverSet) || at(KtTokens.LPAR))) return true
                if (topLevel && atWithRemap(KtTokens.IDENTIFIER)) {
                    return !LAST_DOT_AFTER_RECEIVER_NOT_LPAR_PATTERN_SET.contains(lookahead(1))
                }
                return false
            }
        })

    /*
     * [start] kotlinFile
     *   : preamble toplevelObject* [eof]
     *   ;
     */
    fun parseFile() {
        val fileMarker = mark()

        parsePreamble()

        while (!eof()) {
            parseTopLevelDeclaration()
        }

        checkUnclosedBlockComment()
        fileMarker.done(KtNodeTypes.KT_FILE)
    }

    private fun checkUnclosedBlockComment() {
        if (BLOCK_DOC_COMMENT_SET.contains(builder.rawLookup(-1))) {
            val startOffset = builder.rawTokenTypeStart(-1)
            val endOffset = builder.rawTokenTypeStart(0)
            val tokenChars = builder.text.subSequence(startOffset, endOffset)
            if (!(tokenChars.length > 2 && tokenChars.subSequence(tokenChars.length - 2, tokenChars.length) == "*/")) {
                val marker = builder.mark()
                marker.error("Unclosed comment")
                marker.setCustomEdgeTokenBinders(WhitespacesBinders.greedyRightBinder(), null)
            }
        }
    }

    fun parseTypeCodeFragment() {
        val marker = mark()
        parseTypeRef()

        checkForUnexpectedSymbols()

        marker.done(KtNodeTypes.TYPE_CODE_FRAGMENT)
    }

    fun parseExpressionCodeFragment() {
        val marker = mark()
        expressionParsing.parseExpression()

        checkForUnexpectedSymbols()

        marker.done(KtNodeTypes.EXPRESSION_CODE_FRAGMENT)
    }

    fun parseBlockCodeFragment() {
        val marker = mark()
        val blockMarker = mark()

        if (at(KtTokens.PACKAGE_KEYWORD) || atWithRemap(KtTokens.IMPORT_KEYWORD)) {
            val err = mark()
            parsePreamble()
            err.error("Package directive and imports are forbidden in code fragments")
        }

        expressionParsing.parseStatements()

        checkForUnexpectedSymbols()

        blockMarker.done(KtNodeTypes.BLOCK)
        marker.done(KtNodeTypes.BLOCK_CODE_FRAGMENT)
    }

    fun parseLambdaExpression() {
        expressionParsing.parseFunctionLiteral(preferBlock = false, collapse = false)
    }

    fun parseBlockExpression() {
        parseBlock(collapse = false)
    }

    fun parseScript() {
        val fileMarker = mark()

        parsePreamble()

        val scriptMarker = mark()

        val blockMarker = mark()

        expressionParsing.parseStatements(isScriptTopLevel = true)

        checkForUnexpectedSymbols()

        blockMarker.done(KtNodeTypes.BLOCK)
        blockMarker.setCustomEdgeTokenBinders(PRECEDING_ALL_BINDER, TRAILING_ALL_BINDER)

        scriptMarker.done(KtNodeTypes.SCRIPT)
        scriptMarker.setCustomEdgeTokenBinders(PRECEDING_ALL_BINDER, TRAILING_ALL_BINDER)

        fileMarker.done(KtNodeTypes.KT_FILE)
    }

    private fun checkForUnexpectedSymbols() {
        while (!eof()) {
            errorAndAdvance("Unexpected symbol")
        }
    }

    /*
     * preamble
     *   : fileAnnotationList? packageDirective? import*
     *   ;
     */
    private fun parsePreamble() {
        val firstEntry = mark()

        /*
         * fileAnnotationList
         *   : fileAnnotations*
         */
        parseFileAnnotationList(AnnotationParsingMode.FILE_ANNOTATIONS_BEFORE_PACKAGE)

        /*
         * packageDirective
         *   : modifiers "package" SimpleName{"."} SEMI?
         *   ;
         */
        var packageDirective = mark()
        parseModifierList(emptySyntaxElementTypeSet())

        if (at(KtTokens.PACKAGE_KEYWORD)) {
            advance() // PACKAGE_KEYWORD

            parsePackageName()

            firstEntry.drop()

            consumeIfSemicolon()

            packageDirective.done(KtNodeTypes.PACKAGE_DIRECTIVE)
        } else {
            // When package directive is omitted we should not report error on non-file annotations at the beginning of the file.
            // So, we rollback the parsing position and reparse file annotation list without report error on non-file annotations.
            firstEntry.rollbackTo()

            parseFileAnnotationList(AnnotationParsingMode.FILE_ANNOTATIONS_WHEN_PACKAGE_OMITTED)
            packageDirective = mark()
            packageDirective.done(KtNodeTypes.PACKAGE_DIRECTIVE)
            // Need to skip everything but shebang comment to allow comments at the start of the file to be bound to the first declaration.
            packageDirective.setCustomEdgeTokenBinders(BindFirstShebangWithWhitespaceOnly, null)
        }

        parseImportDirectives()
    }

    /* SimpleName{"."} */
    private fun parsePackageName() {
        var qualifiedExpression = mark()
        var simpleName = true
        while (true) {
            if (builder.newlineBeforeCurrentToken()) {
                errorWithRecovery("Package name must be a '.'-separated identifier list placed on a single line", PACKAGE_NAME_RECOVERY_SET)
                break
            }

            if (at(KtTokens.DOT)) {
                advance() // DOT
                qualifiedExpression.error("Package name must be a '.'-separated identifier list")
                qualifiedExpression = mark()
                continue
            }

            val nsName = mark()
            val simpleNameFound =
                expectIdentifierWithRemap("Package name must be a '.'-separated identifier list", PACKAGE_NAME_RECOVERY_SET)
            if (simpleNameFound) {
                nsName.done(KtNodeTypes.REFERENCE_EXPRESSION)
            } else {
                nsName.drop()
            }

            if (!simpleName) {
                val precedingMarker = qualifiedExpression.precede()
                qualifiedExpression.done(KtNodeTypes.DOT_QUALIFIED_EXPRESSION)
                qualifiedExpression = precedingMarker
            }

            if (at(KtTokens.DOT)) {
                advance() // DOT

                if (simpleName && !simpleNameFound) {
                    qualifiedExpression.drop()
                    qualifiedExpression = mark()
                } else {
                    simpleName = false
                }
            } else {
                break
            }
        }
        qualifiedExpression.drop()
    }

    /*
     * import
     *   : "import" SimpleName{"."} ("." "*" | "as" SimpleName)? SEMI?
     *   ;
     */
    private fun parseImportDirective() {
        require(atWithRemap(KtTokens.IMPORT_KEYWORD))
        val importDirective = mark()
        advance() // IMPORT_KEYWORD

        if (closeImportWithErrorIfNewline(importDirective, null, "Expecting qualified name")) {
            return
        }

        if (!atWithRemap(KtTokens.IDENTIFIER)) {
            val error = mark()
            skipUntil(syntaxElementTypeSetOf(KtTokens.EOL_OR_SEMICOLON))
            error.error("Expecting qualified name")
            importDirective.done(KtNodeTypes.IMPORT_DIRECTIVE)
            consumeIfSemicolon()
            return
        }

        var qualifiedName = mark()
        var reference = mark()
        advance() // IDENTIFIER
        reference.done(KtNodeTypes.REFERENCE_EXPRESSION)

        while (at(KtTokens.DOT) && lookahead(1) !== KtTokens.MUL) {
            advance() // DOT

            if (closeImportWithErrorIfNewline(importDirective, null, "Import must be placed on a single line")) {
                qualifiedName.drop()
                return
            }

            reference = mark()
            if (expectIdentifierWithRemap("Qualified name must be a '.'-separated identifier list", IMPORT_RECOVERY_SET)) {
                reference.done(KtNodeTypes.REFERENCE_EXPRESSION)
            } else {
                reference.drop()
            }

            val precede = qualifiedName.precede()
            qualifiedName.done(KtNodeTypes.DOT_QUALIFIED_EXPRESSION)
            qualifiedName = precede
        }
        qualifiedName.drop()

        if (at(KtTokens.DOT)) {
            advance() // DOT
            require(at(KtTokens.MUL))
            advance() // MUL
            if (at(KtTokens.AS_KEYWORD)) {
                val asMark = mark()
                advance() // AS_KEYWORD
                if (closeImportWithErrorIfNewline(importDirective, null, "Expecting identifier")) {
                    asMark.drop()
                    return
                }
                consumeIfWithRemap(KtTokens.IDENTIFIER)
                asMark.done(KtNodeTypes.IMPORT_ALIAS)
                asMark.precede().error("Cannot rename all imported items to one identifier")
            }
        }
        if (at(KtTokens.AS_KEYWORD)) {
            val alias = mark()
            advance() // AS_KEYWORD
            if (closeImportWithErrorIfNewline(importDirective, alias, "Expecting identifier")) {
                return
            }
            expectIdentifierWithRemap("Expecting identifier", SEMICOLON_SET)
            alias.done(KtNodeTypes.IMPORT_ALIAS)
        }
        consumeIfSemicolon()
        importDirective.done(KtNodeTypes.IMPORT_DIRECTIVE)
        importDirective.setCustomEdgeTokenBinders(null, TrailingCommentsBinder)
    }

    private fun closeImportWithErrorIfNewline(
        importDirective: SyntaxTreeBuilder.Marker,
        importAlias: SyntaxTreeBuilder.Marker?,
        errorMessage: String,
    ): Boolean {
        if (builder.newlineBeforeCurrentToken()) {
            importAlias?.done(KtNodeTypes.IMPORT_ALIAS)
            error(errorMessage)
            importDirective.done(KtNodeTypes.IMPORT_DIRECTIVE)
            return true
        }
        return false
    }

    private fun parseImportDirectives() {
        val importList = mark()
        if (!atWithRemap(KtTokens.IMPORT_KEYWORD)) {
            // this is necessary to allow comments at the start of the file to be bound to the first declaration
            importList.setCustomEdgeTokenBinders(DoNotBindAnything, null)
        }
        while (atWithRemap(KtTokens.IMPORT_KEYWORD)) {
            parseImportDirective()
        }
        importList.done(KtNodeTypes.IMPORT_LIST)
    }

    /*
     * toplevelObject
     *   : package
     *   : class
     *   : extension
     *   : function
     *   : property
     *   : typeAlias
     *   : object
     *   ;
     */
    private fun parseTopLevelDeclaration() {
        if (at(KtTokens.SEMICOLON)) {
            advance() // SEMICOLON
            return
        }
        val decl = mark()

        val detector = ModifierDetector()
        parseModifierList(detector, emptySyntaxElementTypeSet())

        var declType = parseCommonDeclaration(detector, NameParsingMode.REQUIRED, DeclarationParsingMode.MEMBER_OR_TOPLEVEL)

        if (declType == null && at(KtTokens.LBRACE)) {
            error("Expecting a top level declaration")
            parseBlock()
            declType = KtNodeTypes.FUN
        }

        if (declType == null) {
            if (atWithRemap(KtTokens.IMPORT_KEYWORD)) {
                error("imports are only allowed in the beginning of file")
                parseImportDirectives()
            } else {
                errorAndAdvance("Expecting a top level declaration")
            }
            decl.drop()
        } else {
            closeDeclarationWithCommentBinders(decl, declType, true)
        }
    }

    fun parseCommonDeclaration(
        detector: ModifierDetector,
        nameParsingModeForObject: NameParsingMode,
        declarationParsingMode: DeclarationParsingMode,
    ): SyntaxElementType? {
        when (tokenId) {
            KtTokens.CLASS_KEYWORD_ID,
            KtTokens.INTERFACE_KEYWORD_ID,
                -> return parseClass(
                detector.isEnumDetected, true
            )
            KtTokens.FUN_MODIFIER_ID -> return parseFunction()
            KtTokens.VAL_KEYWORD_ID,
            KtTokens.VAR_KEYWORD_ID,
                -> return parseProperty(declarationParsingMode)
            KtTokens.LPAR_ID if lookahead(1) in KtTokens.VAL_VAR -> return parseProperty(declarationParsingMode)
            KtTokens.LBRACKET_ID if lookahead(1) in KtTokens.VAL_VAR -> return parseProperty(declarationParsingMode)
            KtTokens.TYPE_ALIAS_KEYWORD_ID -> return parseTypeAlias()
            KtTokens.OBJECT_KEYWORD_ID -> {
                parseObject(nameParsingModeForObject, true)
                return KtNodeTypes.OBJECT_DECLARATION
            }
            KtTokens.IDENTIFIER_ID -> if (detector.isEnumDetected && declarationParsingMode.canBeEnumUsedAsSoftKeyword) {
                return parseClass(enumClass = true, expectKindKeyword = false)
            }
        }

        return null
    }

    /*
     * (modifier | annotation)*
     */
    fun parseModifierList(noModifiersBefore: SyntaxElementTypeSet): Boolean {
        return parseModifierList(modifierDetector = null, noModifiersBefore = noModifiersBefore)
    }

    fun parseAnnotationsList(noModifiersBefore: SyntaxElementTypeSet) {
        doParseModifierList(
            modifierDetector = null,
            modifierKeywords = emptySyntaxElementTypeSet(),
            annotationParsingMode = AnnotationParsingMode.DEFAULT,
            noModifiersBefore = noModifiersBefore
        )
    }

    /**
     * (modifier | annotation)*
     *
     *
     * Feeds modifiers (not annotations) into the passed consumer if it is not null
     *
     * @param noModifiersBefore is a token set with elements indicating when met them
     * that the previous token must be parsed as an identifier rather than modifier
     *
     * @param localDeclaration is `true` if we are trying to parse a local declaration
     */
    fun parseModifierList(modifierDetector: ModifierDetector?, noModifiersBefore: SyntaxElementTypeSet, localDeclaration: Boolean = false): Boolean {
        return doParseModifierList(modifierDetector, KtTokens.MODIFIERS, AnnotationParsingMode.DEFAULT, noModifiersBefore, localDeclaration)
    }

    private fun parseFunctionTypeValueParameterModifierList() {
        doParseModifierList(
            modifierDetector = null,
            modifierKeywords = KtTokens.RESERVED_VALUE_PARAMETER_MODIFIER_KEYWORDS,
            annotationParsingMode = AnnotationParsingMode.NO_ANNOTATIONS_NO_CONTEXT,
            noModifiersBefore = NO_MODIFIER_BEFORE_FOR_VALUE_PARAMETER
        )
    }

    private fun parseTypeModifierList() {
        doParseModifierList(
            modifierDetector = null,
            modifierKeywords = KtTokens.TYPE_MODIFIER_KEYWORDS,
            annotationParsingMode = AnnotationParsingMode.TYPE_CONTEXT,
            noModifiersBefore = emptySyntaxElementTypeSet()
        )
    }

    private fun parseTypeArgumentModifierList() {
        doParseModifierList(
            null,
            KtTokens.TYPE_ARGUMENT_MODIFIER_KEYWORDS,
            AnnotationParsingMode.NO_ANNOTATIONS_NO_CONTEXT,
            COMMA_COLON_GT_SET
        )
    }

    private fun doParseModifierListBody(
        modifierDetector: ModifierDetector?,
        modifierKeywords: SyntaxElementTypeSet,
        annotationParsingMode: AnnotationParsingMode,
        noModifiersBefore: SyntaxElementTypeSet,
        localDeclaration: Boolean = false,
    ): Boolean {
        var empty = true
        var beforeAnnotationMarker: SyntaxTreeBuilder.Marker?
        while (!eof()) {
            if (at(KtTokens.AT) && annotationParsingMode.allowAnnotations) {
                beforeAnnotationMarker = mark()

                val isAnnotationParsed = parseAnnotationOrList(annotationParsingMode)

                if (!isAnnotationParsed && !annotationParsingMode.withSignificantWhitespaceBeforeArguments) {
                    beforeAnnotationMarker.rollbackTo()
                    // try parse again, but with significant whitespace
                    val newMode =
                        if (annotationParsingMode.allowContextList)
                            AnnotationParsingMode.WITH_SIGNIFICANT_WHITESPACE_BEFORE_ARGUMENTS
                        else
                            AnnotationParsingMode.WITH_SIGNIFICANT_WHITESPACE_BEFORE_ARGUMENTS_NO_CONTEXT
                    doParseModifierListBody(modifierDetector, modifierKeywords, newMode, noModifiersBefore, localDeclaration)
                    empty = false
                    break
                } else {
                    beforeAnnotationMarker.drop()
                }
            } else if (atWithRemap(KtTokens.CONTEXT_KEYWORD) && annotationParsingMode.allowContextList && lookahead(1) === KtTokens.LPAR) {
                val contextMarker = mark()
                if (!parseContextParameterOrReceiverList(false) && localDeclaration) {
                    // Rollback the entire context declaration to make it possible to prevent parsing of potential local declarations
                    // that in fact are not declarations (we are trying to parse declarations at first and statements as second).
                    contextMarker.rollbackTo()
                    break
                } else {
                    contextMarker.drop()
                }
            } else if (tryParseModifier(modifierDetector, noModifiersBefore, modifierKeywords)) {
                // modifier advanced
            } else {
                break
            }
            empty = false
        }

        return empty
    }

    private fun doParseModifierList(
        modifierDetector: ModifierDetector?,
        modifierKeywords: SyntaxElementTypeSet,
        annotationParsingMode: AnnotationParsingMode,
        noModifiersBefore: SyntaxElementTypeSet,
        localDeclaration: Boolean = false,
    ): Boolean {
        val list = mark()

        val empty = doParseModifierListBody(
            modifierDetector,
            modifierKeywords,
            annotationParsingMode,
            noModifiersBefore,
            localDeclaration,
        )

        if (empty) {
            list.drop()
        } else {
            list.done(KtNodeTypes.MODIFIER_LIST)
        }
        return !empty
    }

    private fun tryParseModifier(
        modifierDetector: ModifierDetector?,
        noModifiersBefore: SyntaxElementTypeSet,
        modifierKeywords: SyntaxElementTypeSet,
    ): Boolean {
        val marker = mark()

        if (atSetWithRemap(modifierKeywords)) {
            val lookahead = lookahead(1)

            if (at(KtTokens.FUN_MODIFIER) && lookahead !== KtTokens.INTERFACE_KEYWORD) {
                marker.rollbackTo()
                return false
            }

            if (lookahead != null && !noModifiersBefore.contains(lookahead)) {
                val tt = tt()
                modifierDetector?.consume(tt)
                advance() // MODIFIER
                marker.collapse(tt!!)
                return true
            }
        }

        marker.rollbackTo()
        return false
    }

    /**
     * ```
     * contextReceiverList
     *   : "context" "(" (contextReceiver{","})+ ")"
     * ```
     *
     * @return `true` if it parsed a context with value parameters
     * Otherwise returns `false` if it parsed a context with type refs (that work as receivers) or encountered a syntax error during parsing.
     */
    private fun parseContextParameterOrReceiverList(inFunctionType: Boolean): Boolean {
        require(atWithRemap(KtTokens.CONTEXT_KEYWORD))
        val contextReceiverList = mark()
        advance() // CONTEXT_KEYWORD

        require(at(KtTokens.LPAR))

        val noError: Boolean

        if (lookahead(1) === KtTokens.RPAR) {
            advance() // LPAR
            error("Empty context parameter list")
            advance() // RPAR
            noError = false
        } else {
            // Treat parsing of context receivers (deprecated syntax) as an error,
            // But an outer caller decides if the entire list should be dropped:
            // If we're trying to parse a local declaration, we should drop it to prevent unexpected parsing of ahead declarations
            noError = valueParameterLoop(inFunctionType, CONTEXT_PARAMETERS_FOLLOW_SET) { parseValueParameterOrTypeRef(inFunctionType) }
        }

        contextReceiverList.done(KtNodeTypes.CONTEXT_PARAMETER_LIST)
        return noError
    }

    /**
     * ```
     * contextReceiver
     *   : label? typeReference
     * ```
     *
     * @return `true` if it parsed a value parameter or type ref in the correct position (in function type) and `false` otherwise.
     */
    private fun parseValueParameterOrTypeRef(inFunctionType: Boolean): Boolean {
        if (tryParseValueParameter(true)) {
            return true
        }

        val contextReceiver = mark()
        if (!inFunctionType && expressionParsing.isAtLabelDefinitionOrMissingIdentifier) {
            expressionParsing.parseLabelDefinition()
        }
        parseTypeRef()
        contextReceiver.done(KtNodeTypes.CONTEXT_RECEIVER)
        return inFunctionType
    }

    /*
     * fileAnnotationList
     *   : ("[" "file:" annotationEntry+ "]")*
     *   ;
     */
    private fun parseFileAnnotationList(mode: AnnotationParsingMode) {
        if (!mode.isFileAnnotationParsingMode) {
            // TODO: Log error? (LOG.error("expected file annotation parsing mode, but:$mode"))
        }

        val fileAnnotationsList = mark()

        if (parseAnnotations(mode)) {
            fileAnnotationsList.done(KtNodeTypes.FILE_ANNOTATION_LIST)
        } else {
            fileAnnotationsList.drop()
        }
    }

    /*
     * annotations
     *   : (annotation | annotationList)*
     *   ;
     */
    fun parseAnnotations(mode: AnnotationParsingMode): Boolean {
        if (!parseAnnotationOrList(mode)) return false

        while (parseAnnotationOrList(mode)) {
            // do nothing
        }

        return true
    }

    /*
     * annotation
     *   : "@" (annotationUseSiteTarget ":")? unescapedAnnotation
     *   ;
     *
     * annotationList
     *   : "@" (annotationUseSiteTarget ":")? "[" unescapedAnnotation+ "]"
     *   ;
     *
     * annotationUseSiteTarget
     *   : "file"
     *   : "field"
     *   : "property"
     *   : "get"
     *   : "set"
     *   : "param"
     *   : "setparam"
     *   ;
     */
    private fun parseAnnotationOrList(mode: AnnotationParsingMode): Boolean {
        if (at(KtTokens.AT)) {
            val nextRawToken = builder.rawLookup(1)
            var tokenToMatch = nextRawToken
            var isTargetedAnnotation = false

            if ((nextRawToken === KtTokens.IDENTIFIER || ANNOTATION_TARGETS.contains(nextRawToken)) && lookahead(2) === KtTokens.COLON) {
                tokenToMatch = lookahead(3)
                isTargetedAnnotation = true
            } else if (lookahead(1) === KtTokens.COLON) {
                // recovery for "@:ann"
                isTargetedAnnotation = true
                tokenToMatch = lookahead(2)
            }

            return when (KtTokens.getElementTypeId(tokenToMatch)) {
                KtTokens.IDENTIFIER_ID -> {
                    parseAnnotation(mode)
                }
                KtTokens.LBRACKET_ID -> {
                    parseAnnotationList(mode)
                }
                else -> {
                    if (isTargetedAnnotation) {
                        val advanceTokenCount = if (lookahead(1) === KtTokens.COLON) {
                            2 // AT, COLON
                        } else {
                            3 // AT, (ANNOTATION TARGET KEYWORD), COLON
                        }
                        errorAndAdvance("Expected annotation identifier after ':'", advanceTokenCount)
                    } else {
                        errorAndAdvance("Expected annotation identifier after '@'", 1) // AT
                    }
                    true
                }
            }
        }

        return false
    }

    private fun parseAnnotationList(mode: AnnotationParsingMode): Boolean {
        require(at(KtTokens.AT))
        val annotation = mark()

        builder.disableNewlines()

        advance() // AT

        if (!parseAnnotationTargetIfNeeded(mode)) {
            annotation.rollbackTo()
            builder.restoreNewlinesState()
            return false
        }

        require(at(KtTokens.LBRACKET))
        advance() // LBRACKET

        if (!atWithRemap(KtTokens.IDENTIFIER) && !at(KtTokens.AT)) {
            error("Expecting a list of annotations")
        } else {
            while (atWithRemap(KtTokens.IDENTIFIER) || at(KtTokens.AT)) {
                if (at(KtTokens.AT)) {
                    errorAndAdvance("No '@' needed in annotation list") // AT
                    continue
                }

                parseAnnotation(AnnotationParsingMode.DEFAULT)
                while (at(KtTokens.COMMA)) {
                    errorAndAdvance("No commas needed to separate annotations")
                }
            }
        }

        expect(KtTokens.RBRACKET, "Expecting ']' to close the annotation list")
        builder.restoreNewlinesState()

        annotation.done(KtNodeTypes.ANNOTATION)
        return true
    }

    // Returns true if we should continue parse annotation
    private fun parseAnnotationTargetIfNeeded(mode: AnnotationParsingMode): Boolean {
        val expectedAnnotationTargetBeforeColon = "Expected annotation target before ':'"

        if (at(KtTokens.COLON)) {
            // recovery for "@:ann"
            errorAndAdvance(expectedAnnotationTargetBeforeColon) // COLON
            return true
        }

        val targetKeyword = if (atSetWithRemap(ANNOTATION_TARGETS)) {
            builder.tokenType
        } else {
            null
        }
        if (mode == AnnotationParsingMode.FILE_ANNOTATIONS_WHEN_PACKAGE_OMITTED &&
            !(targetKeyword === KtTokens.FILE_KEYWORD && lookahead(1) === KtTokens.COLON)
        ) {
            return false
        }

        if (lookahead(1) === KtTokens.COLON && targetKeyword == null && atWithRemap(KtTokens.IDENTIFIER)) {
            // recovery for "@fil:ann"
            errorAndAdvance(expectedAnnotationTargetBeforeColon) // IDENTIFIER
            advance() // COLON
            return true
        }

        if (targetKeyword == null && mode.isFileAnnotationParsingMode) {
            parseAnnotationTarget(KtTokens.FILE_KEYWORD)
        } else if (targetKeyword != null) {
            parseAnnotationTarget(targetKeyword)
        }

        return true
    }

    private fun parseAnnotationTarget(keyword: SyntaxElementType) {
        val marker = mark()

        if (!expectWithRemap(keyword)) {
            errorWithRecovery(ANNOTATION_TARGET_ERROR_MESSAGES.getValue(keyword), recoverySet = null)
            marker.drop()
        } else {
            marker.done(KtNodeTypes.ANNOTATION_TARGET)
        }

        if (!at(KtTokens.COLON)) {
            errorWithRecovery(ANNOTATION_TARGET_ERROR_MESSAGES.getValue(keyword), IDENTIFIER_RBRACKET_LBRACKET_SET)
        } else {
            advance() // Skip COLON
        }
    }

    /*
     * annotation
     *   : "@" (annotationUseSiteTarget ":")? unescapedAnnotation
     *   ;
     *
     * unescapedAnnotation
     *   : SimpleName{"."} typeArguments? valueArguments?
     *   ;
     */
    private fun parseAnnotation(mode: AnnotationParsingMode): Boolean {
        require(
            atWithRemap(KtTokens.IDENTIFIER) ||  // We have "@ann" or "@:ann" or "@ :ann", but not "@ ann"
                    // (it's guaranteed that call sites do not allow the latter case)
                    (at(KtTokens.AT) && (!isNextRawTokenCommentOrWhitespace || lookahead(1) === KtTokens.COLON))
        ) { "Invalid annotation prefix" }

        val annotation = mark()

        val atAt = at(KtTokens.AT)
        if (atAt) {
            advance() // AT
        }

        if (atAt && !parseAnnotationTargetIfNeeded(mode)) {
            annotation.rollbackTo()
            return false
        }

        val reference = mark()
        val typeReference = mark()
        parseUserType()
        typeReference.done(KtNodeTypes.TYPE_REFERENCE)
        reference.done(KtNodeTypes.CONSTRUCTOR_CALLEE)

        parseTypeArgumentList()

        if (at(KtTokens.LPAR) &&
            !KtTokens.VAL_VAR.contains(lookahead(1)) &&
            !(KtTokens.WHITE_SPACE_OR_COMMENT_BIT_SET.contains(builder.rawLookup(-1)) && mode.withSignificantWhitespaceBeforeArguments)
        ) {
            expressionParsing.parseValueArgumentList()

            /*
             * There are two problem cases relating to parsing of annotations on a functional type:
             *  - Annotation on a functional type was parsed correctly with the capture parentheses of the functional type,
             *      e.g. @Anno () -> Unit
             *                    ^ Parse error only here: Type expected
             *  - It wasn't parsed, e.g. @Anno (x: kotlin.Any) -> Unit
             *                                           ^ Parse error: Expecting ')'
             *
             * In both cases, parser should rollback to start parsing of annotation and tries parse it with significant whitespace.
             * A marker is set here which means that we must to rollback.
             */
            if (mode.typeContext && (lastToken !== KtTokens.RPAR || at(KtTokens.ARROW))) {
                annotation.done(KtNodeTypes.ANNOTATION_ENTRY)
                return false
            }
        }
        annotation.done(KtNodeTypes.ANNOTATION_ENTRY)

        return true
    }

    private val isNextRawTokenCommentOrWhitespace: Boolean
        get() = KtTokens.WHITE_SPACE_OR_COMMENT_BIT_SET.contains(builder.rawLookup(1))

    enum class NameParsingMode {
        REQUIRED,
        ALLOWED,
        PROHIBITED
    }

    /*
     * class
     *   : modifiers ("class" | "interface") SimpleName
     *       typeParameters?
     *       primaryConstructor?
     *       (":" annotations delegationSpecifier{","})?
     *       typeConstraints
     *       (classBody? | enumClassBody)
     *   ;
     *
     * primaryConstructor
     *   : (modifiers "constructor")? ("(" functionParameter{","} ")")
     *   ;
     *
     * object
     *   : "object" SimpleName? primaryConstructor? ":" delegationSpecifier{","}? classBody?
     *   ;
     */
    private fun parseClassOrObject(
        isObject: Boolean,
        nameParsingMode: NameParsingMode,
        optionalBody: Boolean,
        enumClass: Boolean,
        expectKindKeyword: Boolean,
    ): SyntaxElementType {
        if (expectKindKeyword) {
            if (isObject) {
                require(at(KtTokens.OBJECT_KEYWORD))
            } else {
                require(atSet(CLASS_INTERFACE_SET))
            }
            advance() // CLASS_KEYWORD, INTERFACE_KEYWORD or OBJECT_KEYWORD
        } else {
            require(enumClass) { "Currently classifiers without class/interface/object are only allowed for enums" }
            error("'class' keyword is expected after 'enum'")
        }

        if (nameParsingMode == NameParsingMode.REQUIRED) {
            expectIdentifierWithRemap("Name expected", CLASS_NAME_RECOVERY_SET)
        } else {
            require(isObject) { "Must be an object to be nameless" }
            if (atWithRemap(KtTokens.IDENTIFIER)) {
                if (nameParsingMode == NameParsingMode.PROHIBITED) {
                    errorAndAdvance("An object expression cannot bind a name")
                } else {
                    require(nameParsingMode == NameParsingMode.ALLOWED)
                    advance()
                }
            }
        }

        val typeParametersDeclared = parseTypeParameterList(TYPE_PARAMETER_GT_RECOVERY_SET)

        val beforeConstructorModifiers = mark()
        val primaryConstructorMarker = mark()
        val hasConstructorModifiers = parseModifierList(emptySyntaxElementTypeSet())

        // Some modifiers found, but no parentheses following: class has already ended, and we are looking at something else
        if (hasConstructorModifiers && !atSetWithRemap(LPAR_LBRACE_COLON_CONSTRUCTOR_KEYWORD_SET)) {
            beforeConstructorModifiers.rollbackTo()
            return if (isObject) KtNodeTypes.OBJECT_DECLARATION else KtNodeTypes.CLASS
        }

        // We are still inside a class declaration
        beforeConstructorModifiers.drop()

        val hasConstructorKeyword = atWithRemap(KtTokens.CONSTRUCTOR_KEYWORD)
        if (hasConstructorKeyword) {
            advance() // CONSTRUCTOR_KEYWORD
        }

        if (at(KtTokens.LPAR)) {
            parseValueParameterList(isFunctionTypeContents = false, typeRequired = true, recoverySet = LBRACE_RBRACE_SET)
            primaryConstructorMarker.done(KtNodeTypes.PRIMARY_CONSTRUCTOR)
        } else if (hasConstructorModifiers || hasConstructorKeyword) {
            // A comprehensive error message for cases like:
            //    class A private : Foo
            // or
            //    class A private {
            primaryConstructorMarker.done(KtNodeTypes.PRIMARY_CONSTRUCTOR)
            if (hasConstructorKeyword) {
                error("Expecting primary constructor parameter list")
            } else {
                error("Expecting 'constructor' keyword")
            }
        } else {
            primaryConstructorMarker.drop()
        }

        if (at(KtTokens.COLON)) {
            advance() // COLON
            parseDelegationSpecifierList()
        }

        val whereMarker = OptionalMarker(isObject)
        parseTypeConstraintsGuarded(typeParametersDeclared)
        whereMarker.error("Where clause is not allowed for objects")

        if (at(KtTokens.LBRACE)) {
            if (enumClass) {
                parseEnumClassBody()
            } else {
                parseClassBody()
            }
        } else if (!optionalBody) {
            val fakeBody = mark()
            error("Expecting a class body")
            fakeBody.done(KtNodeTypes.CLASS_BODY)
        }

        return if (isObject) KtNodeTypes.OBJECT_DECLARATION else KtNodeTypes.CLASS
    }

    private fun parseClass(enumClass: Boolean, expectKindKeyword: Boolean): SyntaxElementType {
        return parseClassOrObject(isObject = false, NameParsingMode.REQUIRED, true, enumClass, expectKindKeyword)
    }

    fun parseObject(nameParsingMode: NameParsingMode, optionalBody: Boolean) {
        parseClassOrObject(isObject = true, nameParsingMode, optionalBody, enumClass = false, expectKindKeyword = true)
    }

    /*
     * enumClassBody
     *   : "{" enumEntries (";" members)? "}"
     *   ;
     */
    private fun parseEnumClassBody() {
        if (!at(KtTokens.LBRACE)) return

        val body = mark()
        builder.enableNewlines()

        advance() // LBRACE

        if (!parseEnumEntries() && !at(KtTokens.RBRACE)) {
            error("Expecting ';' after the last enum entry or '}' to close enum class body")
        }
        parseMembers()
        expect(KtTokens.RBRACE, "Expecting '}' to close enum class body")

        builder.restoreNewlinesState()
        body.done(KtNodeTypes.CLASS_BODY)
    }

    /**
     * enumEntries
     * : enumEntry{","}?
     * ;
     *
     * @return true if enum regular members can follow, false otherwise
     */
    private fun parseEnumEntries(): Boolean {
        while (!eof() && !at(KtTokens.RBRACE)) {
            when (parseEnumEntry()) {
                ParseEnumEntryResult.FAILED ->                     // Special case without any enum entries but with possible members after semicolon
                    if (at(KtTokens.SEMICOLON)) {
                        advance()
                        return true
                    } else {
                        return false
                    }
                ParseEnumEntryResult.NO_DELIMITER -> return false
                ParseEnumEntryResult.COMMA_DELIMITER -> {}
                ParseEnumEntryResult.SEMICOLON_DELIMITER -> return true
            }
        }
        return false
    }

    private enum class ParseEnumEntryResult {
        FAILED,
        NO_DELIMITER,
        COMMA_DELIMITER,
        SEMICOLON_DELIMITER
    }

    /*
     * enumEntry
     *   : modifiers SimpleName ("(" arguments ")")? classBody?
     *   ;
     */
    private fun parseEnumEntry(): ParseEnumEntryResult {
        val entry = mark()

        parseModifierList(COMMA_SEMICOLON_RBRACE_SET)

        if (!atSetWithRemap(SOFT_KEYWORDS_AT_MEMBER_START) && atWithRemap(KtTokens.IDENTIFIER)) {
            advance() // IDENTIFIER

            if (at(KtTokens.LPAR)) {
                // Arguments should be parsed here
                // Also, "fake" constructor call tree is created,
                // with empty type name inside
                val initializerList = mark()
                val delegatorSuperCall = mark()

                val callee = mark()
                val typeReference = mark()
                val type = mark()
                val referenceExpr = mark()
                referenceExpr.done(KtNodeTypes.ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION)
                type.done(KtNodeTypes.USER_TYPE)
                typeReference.done(KtNodeTypes.TYPE_REFERENCE)
                callee.done(KtNodeTypes.CONSTRUCTOR_CALLEE)

                expressionParsing.parseValueArgumentList()
                delegatorSuperCall.done(KtNodeTypes.SUPER_TYPE_CALL_ENTRY)
                initializerList.done(KtNodeTypes.INITIALIZER_LIST)
            }
            if (at(KtTokens.LBRACE)) {
                parseClassBody()
            }
            val commaFound = at(KtTokens.COMMA)
            if (commaFound) {
                advance()
            }
            val semicolonFound = at(KtTokens.SEMICOLON)
            if (semicolonFound) {
                advance()
            }

            // Probably some helper function
            closeDeclarationWithCommentBinders(entry, KtNodeTypes.ENUM_ENTRY, true)
            return if (semicolonFound)
                ParseEnumEntryResult.SEMICOLON_DELIMITER
            else
                (if (commaFound) ParseEnumEntryResult.COMMA_DELIMITER else ParseEnumEntryResult.NO_DELIMITER)
        } else {
            entry.rollbackTo()
            return ParseEnumEntryResult.FAILED
        }
    }

    /*
     * classBody
     *   : ("{" members "}")?
     *   ;
     */
    private fun parseClassBody() {
        val body = mark()

        builder.enableNewlines()

        if (expect(KtTokens.LBRACE, "Expecting a class body")) {
            parseMembers()
            expect(KtTokens.RBRACE, "Missing '}")
        }

        builder.restoreNewlinesState()

        body.done(KtNodeTypes.CLASS_BODY)
    }

    /**
     * members
     * : memberDeclaration*
     * ;
     */
    private fun parseMembers() {
        while (!eof() && !at(KtTokens.RBRACE)) {
            parseMemberDeclaration()
        }
    }

    /*
     * memberDeclaration
     *   : modifiers memberDeclaration'
     *   ;
     *
     * memberDeclaration'
     *   : companionObject
     *   : secondaryConstructor
     *   : function
     *   : property
     *   : class
     *   : extension
     *   : typeAlias
     *   : anonymousInitializer
     *   : object
     *   ;
     */
    private fun parseMemberDeclaration() {
        if (at(KtTokens.SEMICOLON)) {
            advance() // SEMICOLON
            return
        }
        val decl = mark()

        val detector = ModifierDetector()
        parseModifierList(detector, emptySyntaxElementTypeSet())

        val declType = parseMemberDeclarationRest(detector)

        if (declType == null) {
            errorWithRecovery("Expecting member declaration", emptySyntaxElementTypeSet())
            decl.drop()
        } else {
            closeDeclarationWithCommentBinders(decl, declType, true)
        }
    }

    private fun parseMemberDeclarationRest(modifierDetector: ModifierDetector): SyntaxElementType? {
        var declType = parseCommonDeclaration(
            modifierDetector,
            if (modifierDetector.isCompanionDetected) NameParsingMode.ALLOWED else NameParsingMode.REQUIRED,
            DeclarationParsingMode.MEMBER_OR_TOPLEVEL
        )

        if (declType != null) return declType

        if (atWithRemap(KtTokens.INIT_KEYWORD)) {
            advance() // init
            if (at(KtTokens.LBRACE)) {
                parseBlock()
            } else {
                mark().error("Expecting '{' after 'init'")
            }
            declType = KtNodeTypes.CLASS_INITIALIZER
        } else if (atWithRemap(KtTokens.CONSTRUCTOR_KEYWORD)) {
            parseSecondaryConstructor()
            declType = KtNodeTypes.SECONDARY_CONSTRUCTOR
        } else if (at(KtTokens.LBRACE)) {
            error("Expecting member declaration")
            parseBlock()
            declType = KtNodeTypes.FUN
        }
        return declType
    }

    /*
     * secondaryConstructor
     *   : modifiers "constructor" valueParameters (":" constructorDelegationCall)? block
     *
     * constructorDelegationCall
     *   : "this" valueArguments
     *   : "super" valueArguments
     */
    private fun parseSecondaryConstructor() {
        require(atWithRemap(KtTokens.CONSTRUCTOR_KEYWORD))

        advance() // CONSTRUCTOR_KEYWORD

        if (at(KtTokens.LPAR)) {
            parseValueParameterList(isFunctionTypeContents = false, typeRequired = true, recoverySet = VALUE_ARGS_RECOVERY_SET)
        } else {
            errorWithRecovery("Expecting '('", SECONDARY_CONSTRUCTOR_RECOVERY_SET)
        }

        if (at(KtTokens.COLON)) {
            advance() // COLON

            val delegationCall = mark()

            if (at(KtTokens.THIS_KEYWORD) || at(KtTokens.SUPER_KEYWORD)) {
                parseThisOrSuper()
                expressionParsing.parseValueArgumentList()
            } else {
                error("Expecting a 'this' or 'super' constructor call")
                var beforeWrongDelegationCallee: SyntaxTreeBuilder.Marker? = null
                if (!at(KtTokens.LPAR)) {
                    beforeWrongDelegationCallee = mark()
                    advance() // wrong delegation callee
                }
                expressionParsing.parseValueArgumentList()

                if (beforeWrongDelegationCallee != null) {
                    if (at(KtTokens.LBRACE)) {
                        beforeWrongDelegationCallee.drop()
                    } else {
                        beforeWrongDelegationCallee.rollbackTo()
                    }
                }
            }

            delegationCall.done(KtNodeTypes.CONSTRUCTOR_DELEGATION_CALL)
        } else {
            // empty constructor delegation call
            val emptyDelegationCall = mark()
            mark().done(KtNodeTypes.CONSTRUCTOR_DELEGATION_REFERENCE)
            emptyDelegationCall.done(KtNodeTypes.CONSTRUCTOR_DELEGATION_CALL)
        }

        if (at(KtTokens.LBRACE)) {
            parseBlock()
        }
    }

    private fun parseThisOrSuper() {
        require(at(KtTokens.THIS_KEYWORD) || at(KtTokens.SUPER_KEYWORD))
        val mark = mark()

        advance() // THIS_KEYWORD | SUPER_KEYWORD

        mark.done(KtNodeTypes.CONSTRUCTOR_DELEGATION_REFERENCE)
    }

    /*
     * typeAlias
     *   : modifiers "typealias" SimpleName typeParameters? "=" type
     *   ;
     */
    private fun parseTypeAlias(): SyntaxElementType {
        require(at(KtTokens.TYPE_ALIAS_KEYWORD))

        advance() // TYPE_ALIAS_KEYWORD

        expectIdentifierWithRemap("Type name expected", LT_EQ_SEMICOLON_TOP_LEVEL_DECLARATION_FIRST_SET)

        parseTypeParameterList(TYPE_PARAMETER_GT_RECOVERY_SET)

        if (atWithRemap(KtTokens.WHERE_KEYWORD)) {
            val error = mark()
            parseTypeConstraints()
            error.error("Type alias parameters can't have bounds")
        }

        expect(KtTokens.EQ, "Expecting '='", TOP_LEVEL_DECLARATION_FIRST_SEMICOLON_SET)

        parseTypeRef()

        consumeIfSemicolon()

        return KtNodeTypes.TYPEALIAS
    }

    enum class DeclarationParsingMode(
        val destructuringAllowed: Boolean,
        val accessorsAllowed: Boolean,
        val canBeEnumUsedAsSoftKeyword: Boolean,
    ) {
        MEMBER_OR_TOPLEVEL(destructuringAllowed = false, accessorsAllowed = true, canBeEnumUsedAsSoftKeyword = true),
        LOCAL(destructuringAllowed = true, accessorsAllowed = false, canBeEnumUsedAsSoftKeyword = false),
        SCRIPT_TOPLEVEL(destructuringAllowed = true, accessorsAllowed = true, canBeEnumUsedAsSoftKeyword = false)
    }

    /*
     * variableDeclarationEntry
     *   : SimpleName (":" type)?
     *   ;
     *
     * property
     *   : modifiers ("val" | "var")
     *       typeParameters?
     *       (type ".")?
     *       ("(" variableDeclarationEntry{","} ")" | variableDeclarationEntry)
     *       typeConstraints
     *       ("by" | "=" expression SEMI?)?
     *       (getter? setter? | setter? getter?) SEMI?
     *   ;
     */
    fun parseProperty(mode: DeclarationParsingMode): SyntaxElementType {
        val isShortForm = at(KtTokens.VAL_KEYWORD) || at(KtTokens.VAR_KEYWORD)
        if (isShortForm) {
            advance()
        }

        val typeParametersDeclared = at(KtTokens.LT) && parseTypeParameterList(IDENTIFIER_EQ_COLON_SEMICOLON_SET)

        builder.disableJoiningComplexTokens()

        val receiver = mark()
        val receiverTypeDeclared = parseReceiverType("property", PROPERTY_NAME_FOLLOW_SET)

        val multiDeclaration = at(KtTokens.LPAR) || at(KtTokens.LBRACKET)

        errorIf(receiver, multiDeclaration && receiverTypeDeclared, "Receiver type is not allowed on a destructuring declaration")

        val isNameOnTheNextLine = eol()
        val beforeName = mark()

        if (multiDeclaration) {
            val multiDecl = mark()
            parseMultiDeclarationEntry(
                follow = if (isShortForm) PROPERTY_NAME_FOLLOW_SET else DESTRUCTURING_PROPERTY_NAME_FOLLOW_SET,
                recoverySet = PROPERTY_NAME_FOLLOW_MULTI_DECLARATION_RECOVERY_SET,
                mode = if (isShortForm) MultiDeclarationMode.Short else MultiDeclarationMode.Full
            )
            errorIf(multiDecl, !mode.destructuringAllowed, "Destructuring declarations are only allowed for local variables/values")
        } else {
            parseFunctionOrPropertyName(
                receiverTypeDeclared,
                "property",
                PROPERTY_NAME_FOLLOW_SET,
                PROPERTY_NAME_FOLLOW_FUNCTION_OR_PROPERTY_RECOVERY_SET,  /*nameRequired = */
                true
            )
        }

        builder.restoreJoiningComplexTokensState()

        var noTypeReference = true
        if (at(KtTokens.COLON)) {
            noTypeReference = false
            val type = mark()
            advance() // COLON
            parseTypeRef()
            errorIf(type, multiDeclaration, "Type annotations are not allowed on destructuring declarations")
        }

        parseTypeConstraintsGuarded(typeParametersDeclared)

        if (!parsePropertyDelegateOrAssignment() && isNameOnTheNextLine && noTypeReference && !receiverTypeDeclared) {
            // Do not parse property identifier on the next line if declaration is invalid
            // In most cases this identifier relates to next statement/declaration
            if (!multiDeclaration || isShortForm) {
                beforeName.rollbackTo()
                error("Expecting property name or receiver type")
            } else {
                beforeName.drop()
            }

            return if (multiDeclaration) KtNodeTypes.DESTRUCTURING_DECLARATION else KtNodeTypes.PROPERTY
        }

        beforeName.drop()

        if (mode.accessorsAllowed) {
            // It's only needed for non-local properties, because in local ones:
            // "val a = 1; b" must not be an infix call of b on "val ...;"

            builder.enableNewlines()
            val hasNewLineWithSemicolon = consumeIfSemicolon() && builder.newlineBeforeCurrentToken()
            builder.restoreNewlinesState()

            if (!hasNewLineWithSemicolon) {
                val alreadyRead = PropertyComponentKind.Collector()
                var propertyComponentKind = parsePropertyComponent(alreadyRead)

                while (propertyComponentKind != null) {
                    alreadyRead.collect(propertyComponentKind)
                    propertyComponentKind = parsePropertyComponent(alreadyRead)
                }

                if (!atSetWithRemap(EOL_OR_SEMICOLON_RBRACE_SET)) {
                    if (lastToken !== KtTokens.SEMICOLON) {
                        errorUntil("Property getter or setter expected", PROPERTY_GETTER_OR_SETTER_EXPECTED_RECOVERY_SET)
                    }
                } else {
                    consumeIfSemicolon()
                }
            }
        }

        return if (multiDeclaration) KtNodeTypes.DESTRUCTURING_DECLARATION else KtNodeTypes.PROPERTY
    }

    private fun parsePropertyDelegateOrAssignment(): Boolean {
        return when {
            atWithRemap(KtTokens.BY_KEYWORD) -> {
                parsePropertyDelegate()
                true
            }
            at(KtTokens.EQ) -> {
                advance() // EQ
                expressionParsing.parseExpression()
                true
            }
            else -> false
        }
    }

    /*
     * propertyDelegate
     *   : "by" expression
     *   ;
     */
    private fun parsePropertyDelegate() {
        require(atWithRemap(KtTokens.BY_KEYWORD))
        val delegate = mark()
        advance() // BY_KEYWORD
        expressionParsing.parseExpression()
        delegate.done(KtNodeTypes.PROPERTY_DELEGATE)
    }

    enum class MultiDeclarationMode {
        Short,
        Full,
        FullValOnly,
    }

    /*
     * (SimpleName (":" type){","})
     */
    fun parseMultiDeclarationEntry(follow: SyntaxElementTypeSet, recoverySet: SyntaxElementTypeSet, mode: MultiDeclarationMode) {
        // Parsing multi-name, e.g.
        //   val (a, b) = foo()
        //   (val a: X = aa, var b) = foo()
        //   val [a, b] = foo()
        //   [val a: X, var b] = foo()
        builder.disableNewlines()

        val isParentheses = at(KtTokens.LPAR)
        val closingBrace = if (isParentheses) KtTokens.RPAR else KtTokens.RBRACKET

        advance() // LPAR | LBRACKET

        if (!atSetWithRemap(follow)) {
            while (true) {
                if (at(KtTokens.COMMA)) {
                    errorAndAdvance("Expecting a name")
                } else if (at(closingBrace)) { // For declaration similar to `val () = somethingCall()`
                    error("Expecting a name")
                    break
                }
                val property = mark()

                when (mode) {
                    MultiDeclarationMode.Full -> {
                        if (at(KtTokens.VAL_KEYWORD) || at(KtTokens.VAR_KEYWORD)) {
                            advance()
                        } else {
                            errorWithRecovery("Expecting val or var keyword", recoverySet)
                        }
                    }
                    MultiDeclarationMode.FullValOnly -> {
                        if (at(KtTokens.VAL_KEYWORD)) {
                            advance()
                        } else {
                            errorWithRecovery("Expecting val keyword", recoverySet)
                        }
                    }
                    MultiDeclarationMode.Short -> {
                        parseModifierList(COMMA_RPAR_COLON_EQ_SET)
                    }
                }

                expectIdentifierWithRemap("Expecting a name", recoverySet)

                if (at(KtTokens.COLON)) {
                    advance() // COLON
                    parseTypeRef(follow)
                }

                if (at(KtTokens.EQ) && closingBrace == KtTokens.RPAR) {
                    advance()
                    expressionParsing.parseSimpleNameExpression()
                }

                property.done(KtNodeTypes.DESTRUCTURING_DECLARATION_ENTRY)

                if (!at(KtTokens.COMMA)) break
                advance() // COMMA
                if (at(closingBrace)) break
            }
        }

        expect(closingBrace, if (isParentheses) "Expecting '('" else "Expecting '['", follow)
        builder.restoreNewlinesState()
    }

    private enum class PropertyComponentKind {
        GET,
        SET,
        FIELD;

        class Collector {
            private val collected = booleanArrayOf(false, false, false)

            fun collect(kind: PropertyComponentKind) {
                collected[kind.ordinal] = true
            }

            fun contains(kind: PropertyComponentKind): Boolean {
                return collected[kind.ordinal]
            }
        }
    }

    /*
     * propertyComponent
     *   : modifiers ("get" | "set")
     *   :
     *        (     "get" "(" ")"
     *           |
     *              "set" "(" modifiers parameter ")"
     *           |
     *              "field"
     *        ) functionBody
     *   ;
     */
    private fun parsePropertyComponent(notAllowedKind: PropertyComponentKind.Collector): PropertyComponentKind? {
        val propertyComponent = mark()

        parseModifierList(emptySyntaxElementTypeSet())
        val propertyComponentKind = if (atWithRemap(KtTokens.GET_KEYWORD)) {
            PropertyComponentKind.GET
        } else if (atWithRemap(KtTokens.SET_KEYWORD)) {
            PropertyComponentKind.SET
        } else if (atWithRemap(KtTokens.FIELD_KEYWORD)) {
            PropertyComponentKind.FIELD
        } else {
            propertyComponent.rollbackTo()
            return null
        }

        if (notAllowedKind.contains(propertyComponentKind)) {
            propertyComponent.rollbackTo()
            return null
        }

        advance() // GET_KEYWORD, SET_KEYWORD or FIELD_KEYWORD

        if (!at(KtTokens.LPAR) && propertyComponentKind != PropertyComponentKind.FIELD) {
            // Account for Jet-114 (val a : int get {...})
            if (!atSetWithRemap(ACCESSOR_FIRST_OR_PROPERTY_END)) {
                errorUntil("Accessor body expected", ACCESSOR_BODY_EXPECTED_RECOVERY_SET)
            } else {
                closeDeclarationWithCommentBinders(propertyComponent, KtNodeTypes.PROPERTY_ACCESSOR, true)
                return propertyComponentKind
            }
        }

        builder.disableNewlines()

        if (propertyComponentKind != PropertyComponentKind.FIELD) {
            val parameterList = mark()
            expect(KtTokens.LPAR, "Expecting '('", RPAR_IDENTIFIER_COLON_LBRACE_EQ_SET)
            if (propertyComponentKind == PropertyComponentKind.SET) {
                val setterParameter = mark()
                parseModifierList(COMMA_COLON_RPAR_SET)
                expectIdentifierWithRemap("Expecting parameter name", RPAR_COLON_LBRACE_EQ_SET)

                if (at(KtTokens.COLON)) {
                    advance() // COLON
                    parseTypeRef()
                }
                setterParameter.done(KtNodeTypes.VALUE_PARAMETER)
                if (at(KtTokens.COMMA)) {
                    advance() // COMMA
                }
            }
            if (!at(KtTokens.RPAR)) {
                errorUntil("Expecting ')'", PROPERTY_COMPONENT_RECOVERY_SET)
            }
            if (at(KtTokens.RPAR)) {
                advance()
            }
            parameterList.done(KtNodeTypes.VALUE_PARAMETER_LIST)
        }
        builder.restoreNewlinesState()

        if (at(KtTokens.COLON)) {
            advance()

            parseTypeRef()
        }

        if (propertyComponentKind != PropertyComponentKind.FIELD) {
            parseFunctionContract()
            parseFunctionBody()
        } else if (at(KtTokens.EQ)) {
            advance()
            expressionParsing.parseExpression()
            consumeIfSemicolon()
        }

        if (propertyComponentKind == PropertyComponentKind.FIELD) {
            closeDeclarationWithCommentBinders(propertyComponent, KtNodeTypes.BACKING_FIELD, true)
        } else {
            closeDeclarationWithCommentBinders(propertyComponent, KtNodeTypes.PROPERTY_ACCESSOR, true)
        }

        return propertyComponentKind
    }

    private fun parseFunction(): SyntaxElementType? {
        return parseFunction(false)
    }

    /*
     * function
     *   : modifiers "fun" typeParameters?
     *       (type ".")?
     *       SimpleName
     *       typeParameters? functionParameters (":" type)?
     *       typeConstraints
     *       functionBody?
     *   ;
     */
    @Contract("false -> !null")
    fun parseFunction(failIfIdentifierExists: Boolean): SyntaxElementType? {
        require(at(KtTokens.FUN_MODIFIER))

        advance() // FUN_KEYWORD

        // Recovery for the case of class A { fun| }
        if (at(KtTokens.RBRACE)) {
            error("Function body expected")
            return KtNodeTypes.FUN
        }

        var typeParameterListOccurred = false
        if (at(KtTokens.LT)) {
            parseTypeParameterList(LBRACKET_LBRACE_RBRACE_LPAR_SET)
            typeParameterListOccurred = true
        }

        builder.disableJoiningComplexTokens()

        val receiverFound = parseReceiverType("function", FUNCTION_NAME_FOLLOW_SET)

        if (atWithRemap(KtTokens.IDENTIFIER) && failIfIdentifierExists) {
            builder.restoreJoiningComplexTokensState()
            return null
        }

        // function as expression has no name
        parseFunctionOrPropertyName(
            receiverFound,
            "function",
            FUNCTION_NAME_FOLLOW_SET,
            FUNCTION_NAME_RECOVERY_SET,
            nameRequired = false
        )

        builder.restoreJoiningComplexTokensState()

        if (at(KtTokens.LT)) {
            var error = mark()
            parseTypeParameterList(LPAR_VALUE_PARAMETERS_FOLLOW_SET)
            if (typeParameterListOccurred) {
                val finishIndex = builder.rawTokenIndex()
                error.rollbackTo()
                error = mark()
                advance(finishIndex - builder.rawTokenIndex())
                error.error("Only one type parameter list is allowed for a function")
            } else {
                error.drop()
            }
            typeParameterListOccurred = true
        }

        if (at(KtTokens.LPAR)) {
            parseValueParameterList(isFunctionTypeContents = false, typeRequired = false, recoverySet = VALUE_PARAMETERS_FOLLOW_SET)
        } else {
            error("Expecting '('")
        }

        if (at(KtTokens.COLON)) {
            advance() // COLON

            parseTypeRef()
        }

        val functionContractOccurred = parseFunctionContract()

        parseTypeConstraintsGuarded(typeParameterListOccurred)

        if (!functionContractOccurred) {
            parseFunctionContract()
        }

        if (at(KtTokens.SEMICOLON)) {
            advance() // SEMICOLON
        } else if (at(KtTokens.EQ) || at(KtTokens.LBRACE)) {
            parseFunctionBody()
        }

        return KtNodeTypes.FUN
    }

    /*
     *   (type "." | annotations)?
     */
    private fun parseReceiverType(title: String?, nameFollow: SyntaxElementTypeSet?): Boolean {
        val annotations = mark()
        val annotationsPresent = parseAnnotations(AnnotationParsingMode.DEFAULT)
        val lastDot = lastDotAfterReceiver()
        val receiverPresent = lastDot != -1
        if (annotationsPresent) {
            if (receiverPresent) {
                annotations.rollbackTo()
            } else {
                annotations.error("Annotations are not allowed in this position")
            }
        } else {
            annotations.drop()
        }

        if (!receiverPresent) return false

        createTruncatedBuilder(lastDot).parseTypeRefWithoutIntersections()

        if (atSetWithRemap(RECEIVER_TYPE_TERMINATORS)) {
            advance() // expectation
        } else {
            errorWithRecovery("Expecting '.' before a $title name", nameFollow)
        }
        return true
    }

    private fun lastDotAfterReceiver(): Int {
        val pattern = if (at(KtTokens.LPAR)) lastDotAfterReceiverLParPattern else lastDotAfterReceiverNotLParPattern
        pattern.reset()
        return matchTokenStreamPredicate(pattern)
    }

    /*
     * IDENTIFIER
     */
    private fun parseFunctionOrPropertyName(
        receiverFound: Boolean,
        title: String,
        nameFollow: SyntaxElementTypeSet,
        recoverySet: SyntaxElementTypeSet,
        nameRequired: Boolean,
    ) {
        if (!nameRequired && atSetWithRemap(nameFollow)) return  // no name

        if (expectWithRemap(KtTokens.IDENTIFIER)) {
            return
        }

        errorWithRecovery(
            "Expecting " + title + " name" + (if (!receiverFound) " or receiver type" else ""),
            recoverySet
        )
    }

    /*
     * functionBody
     *   : block
     *   : "=" element
     *   ;
     */
    private fun parseFunctionBody() {
        when (tokenId) {
            KtTokens.LBRACE_ID -> {
                parseBlock()
            }
            KtTokens.EQ_ID -> {
                advance() // EQ
                expressionParsing.parseExpression()
                consumeIfSemicolon()
            }
            else -> {
                error("Expecting function body")
            }
        }
    }

    /*
     * block
     *   : "{" (expressions)* "}"
     *   ;
     */
    fun parseBlock() {
        parseBlock(collapse = true)
    }

    private fun parseBlock(collapse: Boolean) {
        val lazyBlock = mark()

        builder.enableNewlines()

        val hasOpeningBrace = expect(KtTokens.LBRACE, "Expecting '{' to open a block")
        val canCollapse = collapse && hasOpeningBrace && isLazy

        if (canCollapse) {
            advanceBalancedBlock()
        } else {
            expressionParsing.parseStatements()
            expect(KtTokens.RBRACE, "Expecting '}'")
        }

        builder.restoreNewlinesState()

        if (canCollapse) {
            lazyBlock.collapse(KtNodeTypes.BLOCK)
        } else {
            lazyBlock.done(KtNodeTypes.BLOCK)
        }
    }

    fun advanceBalancedBlock() {
        var braceCount = 1
        while (!eof()) {
            if (at(KtTokens.LBRACE)) {
                braceCount++
            } else if (at(KtTokens.RBRACE)) {
                braceCount--
            }

            advance()

            if (braceCount == 0) {
                break
            }
        }
    }

    /*
     * delegationSpecifier{","}
     */
    private fun parseDelegationSpecifierList() {
        val list = mark()

        while (true) {
            if (at(KtTokens.COMMA)) {
                errorAndAdvance("Expecting a delegation specifier")
                continue
            }
            parseDelegationSpecifier()
            if (!at(KtTokens.COMMA)) break
            advance() // COMMA
        }

        list.done(KtNodeTypes.SUPER_TYPE_LIST)
    }

    /*
     * delegationSpecifier
     *   : constructorInvocation // type and constructor arguments
     *   : userType
     *   : explicitDelegation
     *   ;
     *
     * explicitDelegation
     *   : userType "by" element
     *   ;
     */
    private fun parseDelegationSpecifier() {
        val delegator = mark()
        val reference = mark()
        parseTypeRef()

        if (atWithRemap(KtTokens.BY_KEYWORD)) {
            reference.drop()
            advance() // BY_KEYWORD
            createForByClause(builder, isLazy).expressionParsing.parseExpression()
            delegator.done(KtNodeTypes.DELEGATED_SUPER_TYPE_ENTRY)
        } else if (at(KtTokens.LPAR)) {
            reference.done(KtNodeTypes.CONSTRUCTOR_CALLEE)
            expressionParsing.parseValueArgumentList()
            delegator.done(KtNodeTypes.SUPER_TYPE_CALL_ENTRY)
        } else {
            reference.drop()
            delegator.done(KtNodeTypes.SUPER_TYPE_ENTRY)
        }
    }

    /*
     * typeParameters
     *   : ("<" typeParameter{","} ">"
     *   ;
     */
    private fun parseTypeParameterList(recoverySet: SyntaxElementTypeSet?): Boolean {
        var result = false
        if (at(KtTokens.LT)) {
            val list = mark()

            builder.disableNewlines()
            advance() // LT

            while (true) {
                if (at(KtTokens.COMMA)) errorAndAdvance("Expecting type parameter declaration")
                parseTypeParameter()

                if (!at(KtTokens.COMMA)) break
                advance() // COMMA
                if (at(KtTokens.GT)) {
                    break
                }
            }

            expect(KtTokens.GT, "Missing '>'", recoverySet)
            builder.restoreNewlinesState()
            result = true

            list.done(KtNodeTypes.TYPE_PARAMETER_LIST)
        }
        return result
    }

    /*
     * typeConstraints
     *   : ("where" typeConstraint{","})?
     *   ;
     */
    private fun parseTypeConstraintsGuarded(typeParameterListOccurred: Boolean) {
        val error = mark()
        val constraints = parseTypeConstraints()
        errorIf(error, constraints && !typeParameterListOccurred, "Type constraints are not allowed when no type parameters declared")
    }

    private fun parseTypeConstraints(): Boolean {
        if (atWithRemap(KtTokens.WHERE_KEYWORD)) {
            parseTypeConstraintList()
            return true
        }
        return false
    }

    /*
     * typeConstraint{","}
     */
    private fun parseTypeConstraintList() {
        require(atWithRemap(KtTokens.WHERE_KEYWORD))

        advance() // WHERE_KEYWORD

        val list = mark()

        while (true) {
            if (at(KtTokens.COMMA)) errorAndAdvance("Type constraint expected")
            parseTypeConstraint()
            if (!at(KtTokens.COMMA)) break
            advance() // COMMA
        }

        list.done(KtNodeTypes.TYPE_CONSTRAINT_LIST)
    }

    /*
     * typeConstraint
     *   : annotations SimpleName ":" type
     *   ;
     */
    private fun parseTypeConstraint() {
        val constraint = mark()

        parseAnnotations(AnnotationParsingMode.DEFAULT)

        val reference = mark()
        if (expectIdentifierWithRemap("Expecting type parameter name", COLON_COMMA_LBRACE_RBRACE_TYPE_REF_FIRST_SET)) {
            reference.done(KtNodeTypes.REFERENCE_EXPRESSION)
        } else {
            reference.drop()
        }

        expect(KtTokens.COLON, "Expecting ':' before the upper bound", LBRACE_RBRACE_TYPE_REF_FIRST_SET)

        parseTypeRef()

        constraint.done(KtNodeTypes.TYPE_CONSTRAINT)
    }

    private fun parseFunctionContract(): Boolean {
        if (atWithRemap(CONTRACT_MODIFIER)) {
            expressionParsing.parseContractDescriptionBlock()
            return true
        }
        return false
    }

    /*
     * typeParameter
     *   : modifiers SimpleName (":" userType)?
     *   ;
     */
    private fun parseTypeParameter() {
        if (atSetWithRemap(TYPE_PARAMETER_GT_RECOVERY_SET)) {
            error("Type parameter declaration expected")
            return
        }

        val mark = mark()

        parseModifierList(GT_COMMA_COLON_SET)

        expectIdentifierWithRemap("Type parameter name expected", emptySyntaxElementTypeSet())

        if (at(KtTokens.COLON)) {
            advance() // COLON
            parseTypeRef()
        }

        mark.done(KtNodeTypes.TYPE_PARAMETER)
    }

    fun parseTypeRefWithoutIntersections() {
        parseTypeRef(emptySyntaxElementTypeSet(), allowSimpleIntersectionTypes = false)
    }

    /*
     * type
     *   : typeModifiers typeReference
     *   ;
     *
     * typeReference
     *   : functionType
     *   : userType
     *   : nullableType
     *   : "dynamic"
     *   ;
     *
     * nullableType
     *   : typeReference "?"
     *   ;
     */
    fun parseTypeRef(extraRecoverySet: SyntaxElementTypeSet = emptySyntaxElementTypeSet()) {
        parseTypeRef(extraRecoverySet, allowSimpleIntersectionTypes = true)
    }

    private fun parseTypeRef(extraRecoverySet: SyntaxElementTypeSet, allowSimpleIntersectionTypes: Boolean) {
        val typeRefMarker = parseTypeRefContents(extraRecoverySet, allowSimpleIntersectionTypes)
        typeRefMarker.done(KtNodeTypes.TYPE_REFERENCE)
    }

    // The extraRecoverySet is needed for the foo(bar<x, 1, y>(z)) case, to tell whether we should stop
    // on expression-indicating symbols or not
    private fun parseTypeRefContents(
        extraRecoverySet: SyntaxElementTypeSet,
        allowSimpleIntersectionTypes: Boolean,
    ): SyntaxTreeBuilder.Marker {
        val typeRefMarker = mark()

        parseTypeModifierList()

        val lookahead = lookahead(1)
        val lookahead2 = lookahead(2)
        var typeBeforeDot = true
        val withContextReceiver = atWithRemap(KtTokens.CONTEXT_KEYWORD) && lookahead === KtTokens.LPAR
        var wasFunctionTypeParsed = false

        val contextReceiversStart = mark()

        if (withContextReceiver) {
            parseContextParameterOrReceiverList(true)
        }

        var typeElementMarker = mark()

        when {
            atWithRemap(KtTokens.IDENTIFIER) &&
                    !(lookahead === KtTokens.DOT && lookahead2 === KtTokens.IDENTIFIER) &&
                    lookahead !== KtTokens.LT
                    && atWithRemap(KtTokens.DYNAMIC_KEYWORD) -> {
                val dynamicType = mark()
                advance() // DYNAMIC_KEYWORD
                dynamicType.done(KtNodeTypes.DYNAMIC_TYPE)
            }
            atWithRemap(KtTokens.IDENTIFIER) || at(KtTokens.PACKAGE_KEYWORD) || atParenthesizedMutableForPlatformTypes(0) -> {
                parseUserType()
            }
            at(KtTokens.LPAR) -> {
                val functionOrParenthesizedType = mark()

                // This may be a function parameter list or just a parenthesized type
                advance() // LPAR
                parseTypeRefContents(
                    emptySyntaxElementTypeSet(),  /* allowSimpleIntersectionTypes */
                    true
                ).drop() // parenthesized types, no reference element around it is needed

                if (at(KtTokens.RPAR) && lookahead(1) !== KtTokens.ARROW) {
                    // It's a parenthesized type
                    //    (A)
                    advance()
                    functionOrParenthesizedType.drop()
                } else {
                    // This must be a function type
                    //   (A, B) -> C
                    // or
                    //   (a : A) -> C
                    functionOrParenthesizedType.rollbackTo()
                    parseFunctionType(contextReceiversStart.precede())
                    wasFunctionTypeParsed = true
                }
            }
            else -> {
                errorWithRecovery("Type expected", TYPE_REF_CONTENTS_RECOVERY_SET + extraRecoverySet)
                typeBeforeDot = false
            }
        }

        // Disabling token merge is required for cases like
        //    Int?.(Foo) -> Bar
        builder.disableJoiningComplexTokens()
        typeElementMarker = parseNullableTypeSuffix(typeElementMarker)
        builder.restoreJoiningComplexTokensState()

        var wasIntersection = false
        if (allowSimpleIntersectionTypes && at(KtTokens.AND)) {
            val leftTypeRef = typeElementMarker

            typeElementMarker = typeElementMarker.precede()
            val intersectionType = leftTypeRef.precede()

            leftTypeRef.done(KtNodeTypes.TYPE_REFERENCE)

            advance() // &
            parseTypeRef(extraRecoverySet, allowSimpleIntersectionTypes = true)

            intersectionType.done(KtNodeTypes.INTERSECTION_TYPE)
            wasIntersection = true
        }

        if (typeBeforeDot && at(KtTokens.DOT) && !wasIntersection && !wasFunctionTypeParsed) {
            // This is a receiver for a function type
            //  A.(B) -> C
            //   ^

            val functionType = contextReceiversStart.precede()

            val receiverTypeRef = typeElementMarker.precede()
            val receiverType = receiverTypeRef.precede()
            receiverTypeRef.done(KtNodeTypes.TYPE_REFERENCE)
            receiverType.done(KtNodeTypes.FUNCTION_TYPE_RECEIVER)

            advance() // DOT

            if (at(KtTokens.LPAR)) {
                parseFunctionType(functionType)
            } else {
                functionType.drop()
                error("Expecting function type")
            }

            wasFunctionTypeParsed = true
        }

        if (withContextReceiver && !wasFunctionTypeParsed) {
            errorWithRecovery("Function type expected", TYPE_REF_CONTENTS_RECOVERY_SET + extraRecoverySet)
        }

        typeElementMarker.drop()
        contextReceiversStart.drop()
        return typeRefMarker
    }

    private fun parseNullableTypeSuffix(typeElementMarker: SyntaxTreeBuilder.Marker): SyntaxTreeBuilder.Marker {
        // ?: is joined regardless of joining state
        var typeElementMarker = typeElementMarker
        while (at(KtTokens.QUEST) && builder.rawLookup(1) !== KtTokens.COLON) {
            val precede = typeElementMarker.precede()
            advance() // QUEST
            typeElementMarker.done(KtNodeTypes.NULLABLE_TYPE)
            typeElementMarker = precede
        }
        return typeElementMarker
    }

    /*
     * userType
     *   : simpleUserType{"."}
     *   ;
     *
     *   recovers on platform types:
     *    - Foo!
     *    - (Mutable)List<Foo>!
     *    - Array<(out) Foo>!
     */
    private fun parseUserType() {
        var userType = mark()

        if (at(KtTokens.PACKAGE_KEYWORD)) {
            val keyword = mark()
            advance() // PACKAGE_KEYWORD
            keyword.error("Expecting an element")
            expect(KtTokens.DOT, "Expecting '.'", USER_TYPE_RECOVERY_SET)
        }

        var reference = mark()
        while (true) {
            recoverOnParenthesizedWordForPlatformTypes(0, "Mutable", true)

            if (expectIdentifierWithRemap("Expecting type name", USER_TYPE_NAME_RECOVERY_SET)) {
                reference.done(KtNodeTypes.REFERENCE_EXPRESSION)
            } else {
                reference.drop()
                break
            }

            parseTypeArgumentList()

            recoverOnPlatformTypeSuffix()

            if (!at(KtTokens.DOT)) {
                break
            }
            if (lookahead(1) === KtTokens.LPAR && !atParenthesizedMutableForPlatformTypes(1)) {
                // This may be a receiver for a function type
                //   Int.(Int) -> Int
                break
            }

            val precede = userType.precede()
            userType.done(KtNodeTypes.USER_TYPE)
            userType = precede

            advance() // DOT
            reference = mark()
        }

        userType.done(KtNodeTypes.USER_TYPE)
    }

    private fun atParenthesizedMutableForPlatformTypes(offset: Int): Boolean {
        return recoverOnParenthesizedWordForPlatformTypes(offset, "Mutable", false)
    }

    private fun recoverOnParenthesizedWordForPlatformTypes(offset: Int, word: String, consume: Boolean): Boolean {
        // Array<(out) Foo>! or (Mutable)List<Bar>!
        if (lookahead(offset) === KtTokens.LPAR &&
            lookahead(offset + 1) === KtTokens.IDENTIFIER &&
            lookahead(offset + 2) === KtTokens.RPAR &&
            lookahead(offset + 3) === KtTokens.IDENTIFIER
        ) {
            val error = mark()

            advance(offset)

            advance() // LPAR
            if (word != builder.tokenText) {
                // something other than "out" / "Mutable"
                error.rollbackTo()
                return false
            } else {
                advance() // IDENTIFIER('out')
                advance() // RPAR

                if (consume) {
                    error.error("Unexpected tokens")
                } else {
                    error.rollbackTo()
                }

                return true
            }
        }
        return false
    }

    private fun recoverOnPlatformTypeSuffix() {
        // Recovery for platform types
        if (at(KtTokens.EXCL)) {
            val error = mark()
            advance() // EXCL
            error.error("Unexpected token")
        }
    }

    /*
     *  (optionalProjection type){","}
     */
    private fun parseTypeArgumentList() {
        if (!at(KtTokens.LT)) return

        val list = mark()

        tryParseTypeArgumentList(emptySyntaxElementTypeSet())

        list.done(KtNodeTypes.TYPE_ARGUMENT_LIST)
    }

    fun tryParseTypeArgumentList(extraRecoverySet: SyntaxElementTypeSet): Boolean {
        builder.disableNewlines()
        advance() // LT

        while (true) {
            val projection = mark()

            recoverOnParenthesizedWordForPlatformTypes(0, "out", true)

            // Currently we do not allow annotations on star projections and probably we should not
            // Annotations on other kinds of type arguments should be parsed as common type annotations (within parseTypeRef call)
            parseTypeArgumentModifierList()

            if (at(KtTokens.MUL)) {
                advance() // MUL
            } else {
                parseTypeRef(extraRecoverySet)
            }
            projection.done(KtNodeTypes.TYPE_PROJECTION)
            if (!at(KtTokens.COMMA)) break
            advance() // COMMA
            if (at(KtTokens.GT)) {
                break
            }
        }

        val atGT = at(KtTokens.GT)
        if (!atGT) {
            error("Expecting a '>'")
        } else {
            advance() // GT
        }
        builder.restoreNewlinesState()
        return atGT
    }

    /*
     * functionType
     *   : (type ".")? "(" parameter{","}? ")" "->" type?
     *   ;
     */
    private fun parseFunctionType(functionType: SyntaxTreeBuilder.Marker) {
        parseFunctionTypeContents(functionType).done(KtNodeTypes.FUNCTION_TYPE)
    }

    private fun parseFunctionTypeContents(functionType: SyntaxTreeBuilder.Marker): SyntaxTreeBuilder.Marker {
        require(at(KtTokens.LPAR)) { tt()!! }

        parseValueParameterList(isFunctionTypeContents = true, typeRequired = true, recoverySet = emptySyntaxElementTypeSet())

        expect(KtTokens.ARROW, "Expecting '->' to specify return type of a function type", TYPE_REF_FIRST)
        parseTypeRef()

        return functionType
    }

    /*
     * functionParameters
     *   : "(" functionParameter{","}? ")"
     *   ;
     *
     * functionParameter
     *   : modifiers functionParameterRest
     *   ;
     *
     * functionParameterRest
     *   : parameter ("=" element)?
     *   ;
     */
    private fun parseValueParameterList(isFunctionTypeContents: Boolean, typeRequired: Boolean, recoverySet: SyntaxElementTypeSet) {
        require(at(KtTokens.LPAR))
        val parameters = mark()

        builder.disableNewlines()

        valueParameterLoop(
            isFunctionTypeContents,
            recoverySet
        ) {
            if (isFunctionTypeContents) {
                if (!tryParseValueParameter(typeRequired)) {
                    val valueParameter = mark()
                    parseFunctionTypeValueParameterModifierList()
                    parseTypeRef()
                    closeDeclarationWithCommentBinders(valueParameter, KtNodeTypes.VALUE_PARAMETER, false)
                }
            } else {
                parseValueParameter(typeRequired)
            }
            return@valueParameterLoop true
        }

        builder.restoreNewlinesState()

        parameters.done(KtNodeTypes.VALUE_PARAMETER_LIST)
    }

    /**
     * @param parseParameter returns `true` if internal parsing is correct
     * @return `true` if the parsing of the entire parameter loop is correct
     */
    private inline fun valueParameterLoop(
        inFunctionTypeContext: Boolean,
        recoverySet: SyntaxElementTypeSet,
        parseParameter: () -> Boolean,
    ): Boolean {
        advance() // LPAR

        var noError = true

        if (!at(KtTokens.RPAR) && !atSetWithRemap(recoverySet)) {
            while (true) {
                val offsetBefore = builder.currentOffset
                if (at(KtTokens.RPAR)) {
                    break
                }

                noError = parseParameter() && noError

                if (at(KtTokens.COMMA)) {
                    advance() // COMMA
                } else if (at(KtTokens.COLON)) {
                    // recovery for the case "fun bar(x: Array<Int> : Int)" when we've just parsed "x: Array<Int>"
                    // error should be reported in the `parseValueParameter` call
                    continue
                } else {
                    if (!at(KtTokens.RPAR)) {
                        error("Expecting comma or ')'")
                        noError = false
                    }
                    if (!atSetWithRemap(if (inFunctionTypeContext) LAMBDA_VALUE_PARAMETER_FIRST else VALUE_PARAMETER_FIRST)) break
                    if (offsetBefore == builder.currentOffset) break
                }
            }
        }

        return expect(KtTokens.RPAR, "Expecting ')'", recoverySet) && noError
    }

    /*
     * functionParameter
     *   : modifiers ("val" | "var")? parameter ("=" element)?
     *   ;
     */
    private fun tryParseValueParameter(typeRequired: Boolean): Boolean {
        return parseValueParameter(rollbackOnFailure = true, typeRequired = typeRequired)
    }

    fun parseValueParameter(typeRequired: Boolean) {
        parseValueParameter(rollbackOnFailure = false, typeRequired = typeRequired)
    }

    private fun parseValueParameter(rollbackOnFailure: Boolean, typeRequired: Boolean): Boolean {
        val parameter = mark()

        parseModifierList(NO_MODIFIER_BEFORE_FOR_VALUE_PARAMETER)

        if (at(KtTokens.VAR_KEYWORD) || at(KtTokens.VAL_KEYWORD)) {
            advance() // VAR_KEYWORD | VAL_KEYWORD
        }

        if (!parseFunctionParameterRest(typeRequired) && rollbackOnFailure) {
            parameter.rollbackTo()
            return false
        }

        closeDeclarationWithCommentBinders(parameter, KtNodeTypes.VALUE_PARAMETER, false)
        return true
    }

    /*
     * functionParameterRest
     *   : parameter ("=" element)?
     *   ;
     */
    private fun parseFunctionParameterRest(typeRequired: Boolean): Boolean {
        var noErrors = true

        if (at(KtTokens.COMMA) || at(KtTokens.RPAR)) {
            error("Expecting a parameter declaration")
            noErrors = false
        } else if (atWithRemap(KtTokens.IDENTIFIER) && lookahead(1) === KtTokens.LT) {
            // Recovery for the case 'fun foo(Array<String>) {}'
            error("Parameter name expected")
            noErrors = false
            parseTypeRef()
        } else if (at(KtTokens.COLON)) {
            // Recovery for the case 'fun foo(: Int) {}'
            error("Parameter name expected")
            // We keep noErrors == true so that unnamed parameters starting with ":" are not rolled back during parsing of functional types
            advance() // COLON
            parseTypeRef()
        } else {
            expectIdentifierWithRemap("Parameter name expected", PARAMETER_NAME_RECOVERY_SET)

            if (at(KtTokens.COLON)) {
                advance() // COLON

                if (atWithRemap(KtTokens.IDENTIFIER) && lookahead(1) === KtTokens.COLON) {
                    // recovery for the case "fun foo(x: y: Int)" when we're at "y: " it's likely that this is a name of the next parameter,
                    // not a type reference of the current one
                    error("Type reference expected")
                    return false
                }

                parseTypeRef()
            } else if (typeRequired) {
                errorWithRecovery("Parameters must have type annotation", PARAMETER_NAME_RECOVERY_SET)
                noErrors = false
            }
        }

        if (at(KtTokens.EQ)) {
            advance() // EQ
            expressionParsing.parseExpression()
        }

        return noErrors
    }

    override fun create(builder: SemanticWhitespaceAwareSyntaxBuilder): KotlinParsing {
        return createForTopLevel(builder)
    }

    class ModifierDetector {
        var isEnumDetected: Boolean = false
            private set
        var isCompanionDetected: Boolean = false
            private set

        fun consume(item: SyntaxElementType?) {
            if (item === KtTokens.ENUM_MODIFIER) {
                this.isEnumDetected = true
            } else if (item === KtTokens.COMPANION_MODIFIER) {
                this.isCompanionDetected = true
            }
        }
    }

    enum class AnnotationParsingMode(
        val isFileAnnotationParsingMode: Boolean,
        val allowAnnotations: Boolean,
        val allowContextList: Boolean,
        val typeContext: Boolean,
        val withSignificantWhitespaceBeforeArguments: Boolean,
    ) {
        DEFAULT(
            isFileAnnotationParsingMode = false,
            allowAnnotations = true,
            allowContextList = true,
            typeContext = false,
            withSignificantWhitespaceBeforeArguments = false
        ),
        FILE_ANNOTATIONS_BEFORE_PACKAGE(
            isFileAnnotationParsingMode = true,
            allowAnnotations = true,
            allowContextList = false,
            typeContext = false,
            withSignificantWhitespaceBeforeArguments = false
        ),
        FILE_ANNOTATIONS_WHEN_PACKAGE_OMITTED(
            isFileAnnotationParsingMode = true,
            allowAnnotations = true,
            allowContextList = false,
            typeContext = false,
            withSignificantWhitespaceBeforeArguments = false
        ),
        TYPE_CONTEXT(
            isFileAnnotationParsingMode = false,
            allowAnnotations = true,
            allowContextList = false,
            typeContext = true,
            withSignificantWhitespaceBeforeArguments = false
        ),
        WITH_SIGNIFICANT_WHITESPACE_BEFORE_ARGUMENTS(
            isFileAnnotationParsingMode = false,
            allowAnnotations = true,
            allowContextList = true,
            typeContext = true,
            withSignificantWhitespaceBeforeArguments = true
        ),
        WITH_SIGNIFICANT_WHITESPACE_BEFORE_ARGUMENTS_NO_CONTEXT(
            isFileAnnotationParsingMode = false,
            allowAnnotations = true,
            allowContextList = false,
            typeContext = true,
            withSignificantWhitespaceBeforeArguments = true
        ),
        NO_ANNOTATIONS_NO_CONTEXT(
            isFileAnnotationParsingMode = false,
            allowAnnotations = false,
            allowContextList = false,
            typeContext = false,
            withSignificantWhitespaceBeforeArguments = false
        )
    }
}
