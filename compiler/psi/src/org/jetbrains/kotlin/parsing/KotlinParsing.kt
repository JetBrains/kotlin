/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
package org.jetbrains.kotlin.parsing

import com.intellij.lang.PsiBuilder
import com.intellij.lang.WhitespacesBinders
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.annotations.Contract
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens

class KotlinParsing private constructor(builder: SemanticWhitespaceAwarePsiBuilder, isTopLevel: Boolean, isLazy: Boolean) :
    AbstractKotlinParsing(builder, isLazy) {
    companion object {
        private val GT_COMMA_COLON_SET = TokenSet.create(KtTokens.GT, KtTokens.COMMA, KtTokens.COLON)
        private val LOG = Logger.getInstance(KotlinParsing::class.java)

        private val TOP_LEVEL_DECLARATION_FIRST = TokenSet.create(
            KtTokens.TYPE_ALIAS_KEYWORD, KtTokens.INTERFACE_KEYWORD, KtTokens.CLASS_KEYWORD, KtTokens.OBJECT_KEYWORD,
            KtTokens.FUN_KEYWORD, KtTokens.VAL_KEYWORD, KtTokens.VAR_KEYWORD, KtTokens.PACKAGE_KEYWORD
        )
        private val TOP_LEVEL_DECLARATION_FIRST_SEMICOLON_SET =
            TokenSet.orSet(TOP_LEVEL_DECLARATION_FIRST, TokenSet.create(KtTokens.SEMICOLON))
        private val LT_EQ_SEMICOLON_TOP_LEVEL_DECLARATION_FIRST_SET =
            TokenSet.orSet(TokenSet.create(KtTokens.LT, KtTokens.EQ, KtTokens.SEMICOLON), TOP_LEVEL_DECLARATION_FIRST)
        private val DECLARATION_FIRST = TokenSet.orSet(
            TOP_LEVEL_DECLARATION_FIRST,
            TokenSet.create(KtTokens.INIT_KEYWORD, KtTokens.GET_KEYWORD, KtTokens.SET_KEYWORD, KtTokens.CONSTRUCTOR_KEYWORD)
        )

        private val CLASS_NAME_RECOVERY_SET = TokenSet.orSet(
            TokenSet.create(KtTokens.LT, KtTokens.LPAR, KtTokens.COLON, KtTokens.LBRACE),
            TOP_LEVEL_DECLARATION_FIRST
        )
        private val TYPE_PARAMETER_GT_RECOVERY_SET =
            TokenSet.create(KtTokens.WHERE_KEYWORD, KtTokens.LPAR, KtTokens.COLON, KtTokens.LBRACE, KtTokens.GT)
        val PARAMETER_NAME_RECOVERY_SET: TokenSet =
            TokenSet.create(KtTokens.COLON, KtTokens.EQ, KtTokens.COMMA, KtTokens.RPAR, KtTokens.VAL_KEYWORD, KtTokens.VAR_KEYWORD)
        private val PACKAGE_NAME_RECOVERY_SET = TokenSet.create(KtTokens.DOT, KtTokens.EOL_OR_SEMICOLON)
        private val IMPORT_RECOVERY_SET = TokenSet.create(KtTokens.AS_KEYWORD, KtTokens.DOT, KtTokens.EOL_OR_SEMICOLON)
        private val TYPE_REF_FIRST =
            TokenSet.create(KtTokens.LBRACKET, KtTokens.IDENTIFIER, KtTokens.LPAR, KtTokens.HASH, KtTokens.DYNAMIC_KEYWORD)
        private val LBRACE_RBRACE_TYPE_REF_FIRST_SET = TokenSet.orSet(TokenSet.create(KtTokens.LBRACE, KtTokens.RBRACE), TYPE_REF_FIRST)
        private val COLON_COMMA_LBRACE_RBRACE_TYPE_REF_FIRST_SET =
            TokenSet.orSet(TokenSet.create(KtTokens.COLON, KtTokens.COMMA, KtTokens.LBRACE, KtTokens.RBRACE), TYPE_REF_FIRST)
        private val RECEIVER_TYPE_TERMINATORS = TokenSet.create(KtTokens.DOT, KtTokens.SAFE_ACCESS)
        private val VALUE_PARAMETER_FIRST = TokenSet.orSet(
            TokenSet.create(KtTokens.IDENTIFIER, KtTokens.LBRACKET, KtTokens.VAL_KEYWORD, KtTokens.VAR_KEYWORD),
            TokenSet.andNot(KtTokens.MODIFIER_KEYWORDS, TokenSet.create(KtTokens.FUN_KEYWORD))
        )
        private val LAMBDA_VALUE_PARAMETER_FIRST = TokenSet.orSet(
            TokenSet.create(KtTokens.IDENTIFIER, KtTokens.LBRACKET),
            TokenSet.andNot(KtTokens.MODIFIER_KEYWORDS, TokenSet.create(KtTokens.FUN_KEYWORD))
        )
        private val SOFT_KEYWORDS_AT_MEMBER_START = TokenSet.create(KtTokens.CONSTRUCTOR_KEYWORD, KtTokens.INIT_KEYWORD)
        private val ANNOTATION_TARGETS = TokenSet.create(
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
        private val BLOCK_DOC_COMMENT_SET = TokenSet.create(KtTokens.BLOCK_COMMENT, KtTokens.DOC_COMMENT)
        private val SEMICOLON_SET = TokenSet.create(KtTokens.SEMICOLON)
        private val COMMA_COLON_GT_SET = TokenSet.create(KtTokens.COMMA, KtTokens.COLON, KtTokens.GT)
        private val IDENTIFIER_RBRACKET_LBRACKET_SET = TokenSet.create(KtTokens.IDENTIFIER, KtTokens.RBRACKET, KtTokens.LBRACKET)
        private val LBRACE_RBRACE_SET = TokenSet.create(KtTokens.LBRACE, KtTokens.RBRACE)
        private val COMMA_SEMICOLON_RBRACE_SET = TokenSet.create(KtTokens.COMMA, KtTokens.SEMICOLON, KtTokens.RBRACE)
        private val VALUE_ARGS_RECOVERY_SET =
            TokenSet.create(KtTokens.LBRACE, KtTokens.SEMICOLON, KtTokens.RPAR, KtTokens.EOL_OR_SEMICOLON, KtTokens.RBRACE)
        private val PROPERTY_NAME_FOLLOW_SET = TokenSet.create(
            KtTokens.COLON,
            KtTokens.EQ,
            KtTokens.LBRACE,
            KtTokens.RBRACE,
            KtTokens.SEMICOLON,
            KtTokens.VAL_KEYWORD,
            KtTokens.VAR_KEYWORD,
            KtTokens.FUN_KEYWORD,
            KtTokens.CLASS_KEYWORD
        )
        private val PROPERTY_NAME_FOLLOW_MULTI_DECLARATION_RECOVERY_SET =
            TokenSet.orSet(PROPERTY_NAME_FOLLOW_SET, PARAMETER_NAME_RECOVERY_SET)
        private val PROPERTY_NAME_FOLLOW_FUNCTION_OR_PROPERTY_RECOVERY_SET =
            TokenSet.orSet(PROPERTY_NAME_FOLLOW_SET, LBRACE_RBRACE_SET, TOP_LEVEL_DECLARATION_FIRST)
        private val IDENTIFIER_EQ_COLON_SEMICOLON_SET =
            TokenSet.create(KtTokens.IDENTIFIER, KtTokens.EQ, KtTokens.COLON, KtTokens.SEMICOLON)
        private val COMMA_RPAR_COLON_EQ_SET = TokenSet.create(KtTokens.COMMA, KtTokens.RPAR, KtTokens.COLON, KtTokens.EQ)
        private val ACCESSOR_FIRST_OR_PROPERTY_END = TokenSet.orSet(
            KtTokens.MODIFIER_KEYWORDS,
            TokenSet.create(
                KtTokens.AT,
                KtTokens.GET_KEYWORD,
                KtTokens.SET_KEYWORD,
                KtTokens.FIELD_KEYWORD,
                KtTokens.EOL_OR_SEMICOLON,
                KtTokens.RBRACE
            )
        )
        private val RPAR_IDENTIFIER_COLON_LBRACE_EQ_SET =
            TokenSet.create(KtTokens.RPAR, KtTokens.IDENTIFIER, KtTokens.COLON, KtTokens.LBRACE, KtTokens.EQ)
        private val COMMA_COLON_RPAR_SET = TokenSet.create(KtTokens.COMMA, KtTokens.COLON, KtTokens.RPAR)
        private val RPAR_COLON_LBRACE_EQ_SET = TokenSet.create(KtTokens.RPAR, KtTokens.COLON, KtTokens.LBRACE, KtTokens.EQ)
        private val LBRACKET_LBRACE_RBRACE_LPAR_SET = TokenSet.create(KtTokens.LBRACKET, KtTokens.LBRACE, KtTokens.RBRACE, KtTokens.LPAR)
        private val FUNCTION_NAME_FOLLOW_SET = TokenSet.create(KtTokens.LT, KtTokens.LPAR, KtTokens.RPAR, KtTokens.COLON, KtTokens.EQ)
        private val FUNCTION_NAME_RECOVERY_SET = TokenSet.orSet(
            TokenSet.create(KtTokens.LT, KtTokens.LPAR, KtTokens.RPAR, KtTokens.COLON, KtTokens.EQ),
            LBRACE_RBRACE_SET,
            TOP_LEVEL_DECLARATION_FIRST
        )
        private val VALUE_PARAMETERS_FOLLOW_SET =
            TokenSet.create(KtTokens.EQ, KtTokens.LBRACE, KtTokens.RBRACE, KtTokens.SEMICOLON, KtTokens.RPAR)
        private val CONTEXT_PARAMETERS_FOLLOW_SET = TokenSet.create(
            KtTokens.CLASS_KEYWORD,
            KtTokens.OBJECT_KEYWORD,
            KtTokens.FUN_KEYWORD,
            KtTokens.VAL_KEYWORD,
            KtTokens.VAR_KEYWORD
        )
        private val LPAR_VALUE_PARAMETERS_FOLLOW_SET = TokenSet.orSet(TokenSet.create(KtTokens.LPAR), VALUE_PARAMETERS_FOLLOW_SET)
        private val LPAR_LBRACE_COLON_CONSTRUCTOR_KEYWORD_SET =
            TokenSet.create(KtTokens.LPAR, KtTokens.LBRACE, KtTokens.COLON, KtTokens.CONSTRUCTOR_KEYWORD)
        private val definitelyOutOfReceiverSet = TokenSet.orSet(
            TokenSet.create(KtTokens.EQ, KtTokens.COLON, KtTokens.LBRACE, KtTokens.RBRACE, KtTokens.BY_KEYWORD),
            TOP_LEVEL_DECLARATION_FIRST
        )
        private val EOL_OR_SEMICOLON_RBRACE_SET = TokenSet.create(KtTokens.EOL_OR_SEMICOLON, KtTokens.RBRACE)
        private val CLASS_INTERFACE_SET = TokenSet.create(KtTokens.CLASS_KEYWORD, KtTokens.INTERFACE_KEYWORD)

        @JvmStatic
        fun createForTopLevel(builder: SemanticWhitespaceAwarePsiBuilder): KotlinParsing {
            return KotlinParsing(builder, isTopLevel = true, isLazy = true)
        }

        @JvmStatic
        fun createForTopLevelNonLazy(builder: SemanticWhitespaceAwarePsiBuilder): KotlinParsing {
            return KotlinParsing(builder, isTopLevel = true, isLazy = false)
        }

        private fun createForByClause(builder: SemanticWhitespaceAwarePsiBuilder?, isLazy: Boolean): KotlinParsing {
            return KotlinParsing(SemanticWhitespaceAwarePsiBuilderForByClause(builder), false, isLazy)
        }

        private val NO_MODIFIER_BEFORE_FOR_VALUE_PARAMETER = TokenSet.create(KtTokens.COMMA, KtTokens.COLON, KtTokens.EQ, KtTokens.RPAR)
    }

    private val myExpressionParsing: KotlinExpressionParsing = if (isTopLevel)
        KotlinExpressionParsing(builder, this, isLazy)
    else
        object : KotlinExpressionParsing(builder, this@KotlinParsing, isLazy) {
            override fun parseCallWithClosure(): Boolean {
                if ((builder as SemanticWhitespaceAwarePsiBuilderForByClause).stackSize > 0) {
                    return super.parseCallWithClosure()
                }
                return false
            }

            override fun create(builder: SemanticWhitespaceAwarePsiBuilder): KotlinParsing {
                return createForByClause(builder, isLazy)
            }
        }

    private val lastDotAfterReceiverLParPattern = FirstBefore(
        AtSet(RECEIVER_TYPE_TERMINATORS),
        object : AbstractTokenStreamPredicate() {
            override fun matching(topLevel: Boolean): Boolean {
                if (topLevel && atSet(definitelyOutOfReceiverSet)) {
                    return true
                }
                return topLevel && !at(KtTokens.QUEST) && !at(KtTokens.LPAR) && !at(KtTokens.RPAR)
            }
        }
    )

    private val lastDotAfterReceiverNotLParPattern = LastBefore(
        AtSet(RECEIVER_TYPE_TERMINATORS),
        object : AbstractTokenStreamPredicate() {
            override fun matching(topLevel: Boolean): Boolean {
                if (topLevel && (atSet(definitelyOutOfReceiverSet) || at(KtTokens.LPAR))) return true
                if (topLevel && at(KtTokens.IDENTIFIER)) {
                    val lookahead = lookahead(1)
                    return lookahead !== KtTokens.LT && lookahead !== KtTokens.DOT && lookahead !== KtTokens.SAFE_ACCESS && lookahead !== KtTokens.QUEST
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
        if (BLOCK_DOC_COMMENT_SET.contains(myBuilder.rawLookup(-1))) {
            val startOffset = myBuilder.rawTokenTypeStart(-1)
            val endOffset = myBuilder.rawTokenTypeStart(0)
            val tokenChars = myBuilder.originalText.subSequence(startOffset, endOffset)
            if (!(tokenChars.length > 2 && tokenChars.subSequence(tokenChars.length - 2, tokenChars.length).toString() == "*/")) {
                val marker = myBuilder.mark()
                marker.error("Unclosed comment")
                marker.setCustomEdgeTokenBinders(WhitespacesBinders.GREEDY_RIGHT_BINDER, null)
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
        myExpressionParsing.parseExpression()

        checkForUnexpectedSymbols()

        marker.done(KtNodeTypes.EXPRESSION_CODE_FRAGMENT)
    }

    fun parseBlockCodeFragment() {
        val marker = mark()
        val blockMarker = mark()

        if (at(KtTokens.PACKAGE_KEYWORD) || at(KtTokens.IMPORT_KEYWORD)) {
            val err = mark()
            parsePreamble()
            err.error("Package directive and imports are forbidden in code fragments")
        }

        myExpressionParsing.parseStatements()

        checkForUnexpectedSymbols()

        blockMarker.done(KtNodeTypes.BLOCK)
        marker.done(KtNodeTypes.BLOCK_CODE_FRAGMENT)
    }

    fun parseLambdaExpression() {
        myExpressionParsing.parseFunctionLiteral(preferBlock = false, collapse = false)
    }

    fun parseBlockExpression() {
        parseBlock(collapse = false)
    }

    fun parseScript() {
        val fileMarker = mark()

        parsePreamble()

        val scriptMarker = mark()

        val blockMarker = mark()

        myExpressionParsing.parseStatements(isScriptTopLevel = true)

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
     *preamble
     *  : fileAnnotationList? packageDirective? import*
     *  ;
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
        parseModifierList(TokenSet.EMPTY)

        if (at(KtTokens.PACKAGE_KEYWORD)) {
            advance() // PACKAGE_KEYWORD

            parsePackageName()

            firstEntry.drop()

            consumeIf(KtTokens.SEMICOLON)

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
            if (myBuilder.newlineBeforeCurrentToken()) {
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
                expect(KtTokens.IDENTIFIER, "Package name must be a '.'-separated identifier list", PACKAGE_NAME_RECOVERY_SET)
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
        assert(_at(KtTokens.IMPORT_KEYWORD))
        val importDirective = mark()
        advance() // IMPORT_KEYWORD

        if (closeImportWithErrorIfNewline(importDirective, null, "Expecting qualified name")) {
            return
        }

        if (!at(KtTokens.IDENTIFIER)) {
            val error = mark()
            skipUntil(TokenSet.create(KtTokens.EOL_OR_SEMICOLON))
            error.error("Expecting qualified name")
            importDirective.done(KtNodeTypes.IMPORT_DIRECTIVE)
            consumeIf(KtTokens.SEMICOLON)
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
            if (expect(KtTokens.IDENTIFIER, "Qualified name must be a '.'-separated identifier list", IMPORT_RECOVERY_SET)) {
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
            assert(_at(KtTokens.MUL))
            advance() // MUL
            if (at(KtTokens.AS_KEYWORD)) {
                val `as` = mark()
                advance() // AS_KEYWORD
                if (closeImportWithErrorIfNewline(importDirective, null, "Expecting identifier")) {
                    `as`.drop()
                    return
                }
                consumeIf(KtTokens.IDENTIFIER)
                `as`.done(KtNodeTypes.IMPORT_ALIAS)
                `as`.precede().error("Cannot rename all imported items to one identifier")
            }
        }
        if (at(KtTokens.AS_KEYWORD)) {
            val alias = mark()
            advance() // AS_KEYWORD
            if (closeImportWithErrorIfNewline(importDirective, alias, "Expecting identifier")) {
                return
            }
            expect(KtTokens.IDENTIFIER, "Expecting identifier", SEMICOLON_SET)
            alias.done(KtNodeTypes.IMPORT_ALIAS)
        }
        consumeIf(KtTokens.SEMICOLON)
        importDirective.done(KtNodeTypes.IMPORT_DIRECTIVE)
        importDirective.setCustomEdgeTokenBinders(null, TrailingCommentsBinder)
    }

    private fun closeImportWithErrorIfNewline(
        importDirective: PsiBuilder.Marker,
        importAlias: PsiBuilder.Marker?,
        errorMessage: String
    ): Boolean {
        if (myBuilder.newlineBeforeCurrentToken()) {
            importAlias?.done(KtNodeTypes.IMPORT_ALIAS)
            error(errorMessage)
            importDirective.done(KtNodeTypes.IMPORT_DIRECTIVE)
            return true
        }
        return false
    }

    private fun parseImportDirectives() {
        val importList = mark()
        if (!at(KtTokens.IMPORT_KEYWORD)) {
            // this is necessary to allow comments at the start of the file to be bound to the first declaration
            importList.setCustomEdgeTokenBinders(DoNotBindAnything, null)
        }
        while (at(KtTokens.IMPORT_KEYWORD)) {
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
        parseModifierList(detector, TokenSet.EMPTY)

        var declType = parseCommonDeclaration(detector, NameParsingMode.REQUIRED, DeclarationParsingMode.MEMBER_OR_TOPLEVEL)

        if (declType == null && at(KtTokens.LBRACE)) {
            error("Expecting a top level declaration")
            parseBlock()
            declType = KtNodeTypes.FUN
        }

        when (declType) {
            null if at(KtTokens.IMPORT_KEYWORD) -> {
                error("imports are only allowed in the beginning of file")
                parseImportDirectives()
                decl.drop()
            }
            null -> {
                errorAndAdvance("Expecting a top level declaration")
                decl.drop()
            }
            else -> {
                closeDeclarationWithCommentBinders(decl, declType, true)
            }
        }
    }

    fun parseCommonDeclaration(
        detector: ModifierDetector,
        nameParsingModeForObject: NameParsingMode,
        declarationParsingMode: DeclarationParsingMode
    ): IElementType? {
        when (tokenId) {
            KtTokens.CLASS_KEYWORD_Id, KtTokens.INTERFACE_KEYWORD_Id -> return parseClass(
                detector.isEnumDetected, true
            )
            KtTokens.FUN_KEYWORD_Id -> return parseFunction()
            KtTokens.VAL_KEYWORD_Id, KtTokens.VAR_KEYWORD_Id -> return parseProperty(declarationParsingMode)
            KtTokens.TYPE_ALIAS_KEYWORD_Id -> return parseTypeAlias()
            KtTokens.OBJECT_KEYWORD_Id -> {
                parseObject(nameParsingModeForObject, true)
                return KtNodeTypes.OBJECT_DECLARATION
            }
            KtTokens.IDENTIFIER_Id -> if (detector.isEnumDetected && declarationParsingMode.canBeEnumUsedAsSoftKeyword) {
                return parseClass(enumClass = true, expectKindKeyword = false)
            }
        }

        return null
    }

    /*
     * (modifier | annotation)*
     */
    fun parseModifierList(noModifiersBefore: TokenSet): Boolean {
        return parseModifierList(null, noModifiersBefore)
    }

    fun parseAnnotationsList(noModifiersBefore: TokenSet) {
        doParseModifierList(null, TokenSet.EMPTY, AnnotationParsingMode.DEFAULT, noModifiersBefore)
    }

    /**
     * (modifier | annotation)*
     *
     *
     * Feeds modifiers (not annotations) into the passed consumer, if it is not null
     *
     * @param noModifiersBefore is a token set with elements indicating when met them
     * that previous token must be parsed as an identifier rather than modifier
     */
    fun parseModifierList(tokenConsumer: Consumer<IElementType?>?, noModifiersBefore: TokenSet): Boolean {
        return doParseModifierList(tokenConsumer, KtTokens.MODIFIER_KEYWORDS, AnnotationParsingMode.DEFAULT, noModifiersBefore)
    }

    private fun parseFunctionTypeValueParameterModifierList() {
        doParseModifierList(
            null,
            KtTokens.RESERVED_VALUE_PARAMETER_MODIFIER_KEYWORDS,
            AnnotationParsingMode.NO_ANNOTATIONS_NO_CONTEXT,
            NO_MODIFIER_BEFORE_FOR_VALUE_PARAMETER
        )
    }

    private fun parseTypeModifierList() {
        doParseModifierList(null, KtTokens.TYPE_MODIFIER_KEYWORDS, AnnotationParsingMode.TYPE_CONTEXT, TokenSet.EMPTY)
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
        tokenConsumer: Consumer<IElementType?>?,
        modifierKeywords: TokenSet,
        annotationParsingMode: AnnotationParsingMode,
        noModifiersBefore: TokenSet
    ): Boolean {
        var empty = true
        var beforeAnnotationMarker: PsiBuilder.Marker?
        while (!eof()) {
            if (at(KtTokens.AT) && annotationParsingMode.allowAnnotations) {
                beforeAnnotationMarker = mark()

                val isAnnotationParsed = parseAnnotationOrList(annotationParsingMode)

                if (!isAnnotationParsed && !annotationParsingMode.withSignificantWhitespaceBeforeArguments) {
                    beforeAnnotationMarker.rollbackTo()
                    // try parse again, but with significant whitespace
                    val newMode =
                        if (annotationParsingMode.allowContextList) AnnotationParsingMode.WITH_SIGNIFICANT_WHITESPACE_BEFORE_ARGUMENTS else AnnotationParsingMode.WITH_SIGNIFICANT_WHITESPACE_BEFORE_ARGUMENTS_NO_CONTEXT
                    doParseModifierListBody(tokenConsumer, modifierKeywords, newMode, noModifiersBefore)
                    empty = false
                    break
                } else {
                    beforeAnnotationMarker.drop()
                }
            } else if (at(KtTokens.CONTEXT_KEYWORD) && annotationParsingMode.allowContextList && lookahead(1) === KtTokens.LPAR) {
                parseContextReceiverList(false)
            } else if (tryParseModifier(tokenConsumer, noModifiersBefore, modifierKeywords)) {
                // modifier advanced
            } else {
                break
            }
            empty = false
        }

        return empty
    }

    private fun doParseModifierList(
        tokenConsumer: Consumer<IElementType?>?,
        modifierKeywords: TokenSet,
        annotationParsingMode: AnnotationParsingMode,
        noModifiersBefore: TokenSet
    ): Boolean {
        val list = mark()

        val empty = doParseModifierListBody(
            tokenConsumer,
            modifierKeywords,
            annotationParsingMode,
            noModifiersBefore
        )

        if (empty) {
            list.drop()
        } else {
            list.done(KtNodeTypes.MODIFIER_LIST)
        }
        return !empty
    }

    private fun tryParseModifier(
        tokenConsumer: Consumer<IElementType?>?,
        noModifiersBefore: TokenSet,
        modifierKeywords: TokenSet
    ): Boolean {
        val marker = mark()

        if (atSet(modifierKeywords)) {
            val lookahead = lookahead(1)

            if (at(KtTokens.FUN_KEYWORD) && lookahead !== KtTokens.INTERFACE_KEYWORD) {
                marker.rollbackTo()
                return false
            }

            if (lookahead != null && !noModifiersBefore.contains(lookahead)) {
                val tt = tt()
                tokenConsumer?.consume(tt)
                advance() // MODIFIER
                marker.collapse(tt!!)
                return true
            }
        }

        marker.rollbackTo()
        return false
    }

    /*
     * contextReceiverList
     *   : "context" "(" (contextReceiver{","})+ ")"
     */
    private fun parseContextReceiverList(inFunctionType: Boolean) {
        assert(_at(KtTokens.CONTEXT_KEYWORD))
        val contextReceiverList = mark()
        advance() // CONTEXT_KEYWORD

        assert(_at(KtTokens.LPAR))

        if (lookahead(1) === KtTokens.RPAR) {
            advance() // LPAR
            error("Empty context parameter list")
            advance() // RPAR
        } else {
            valueParameterLoop(inFunctionType, CONTEXT_PARAMETERS_FOLLOW_SET) { parseContextReceiver(inFunctionType) }
        }

        contextReceiverList.done(KtNodeTypes.CONTEXT_RECEIVER_LIST)
    }

    /*
     * contextReceiver
     *   : label? typeReference
     */
    private fun parseContextReceiver(inFunctionType: Boolean) {
        if (tryParseValueParameter(true)) {
            return
        }

        val contextReceiver = mark()
        if (!inFunctionType && myExpressionParsing.isAtLabelDefinitionOrMissingIdentifier) {
            myExpressionParsing.parseLabelDefinition()
        }
        parseTypeRef()
        contextReceiver.done(KtNodeTypes.CONTEXT_RECEIVER)
    }

    /*
     * fileAnnotationList
     *   : ("[" "file:" annotationEntry+ "]")*
     *   ;
     */
    private fun parseFileAnnotationList(mode: AnnotationParsingMode) {
        if (!mode.isFileAnnotationParsingMode) {
            LOG.error("expected file annotation parsing mode, but:$mode")
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
            val nextRawToken = myBuilder.rawLookup(1)
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

            if (tokenToMatch === KtTokens.IDENTIFIER) {
                return parseAnnotation(mode)
            } else if (tokenToMatch === KtTokens.LBRACKET) {
                return parseAnnotationList(mode)
            } else {
                if (isTargetedAnnotation) {
                    if (lookahead(1) === KtTokens.COLON) {
                        errorAndAdvance("Expected annotation identifier after ':'", 2) // AT, COLON
                    } else {
                        errorAndAdvance("Expected annotation identifier after ':'", 3) // AT, (ANNOTATION TARGET KEYWORD), COLON
                    }
                } else {
                    errorAndAdvance("Expected annotation identifier after '@'", 1) // AT
                }
            }
            return true
        }

        return false
    }

    private fun parseAnnotationList(mode: AnnotationParsingMode): Boolean {
        assert(_at(KtTokens.AT))
        val annotation = mark()

        myBuilder.disableNewlines()

        advance() // AT

        if (!parseAnnotationTargetIfNeeded(mode)) {
            annotation.rollbackTo()
            myBuilder.restoreNewlinesState()
            return false
        }

        assert(_at(KtTokens.LBRACKET))
        advance() // LBRACKET

        if (!at(KtTokens.IDENTIFIER) && !at(KtTokens.AT)) {
            error("Expecting a list of annotations")
        } else {
            while (at(KtTokens.IDENTIFIER) || at(KtTokens.AT)) {
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
        myBuilder.restoreNewlinesState()

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

        val targetKeyword = atTargetKeyword()
        if (mode == AnnotationParsingMode.FILE_ANNOTATIONS_WHEN_PACKAGE_OMITTED && !(targetKeyword === KtTokens.FILE_KEYWORD && lookahead(1) === KtTokens.COLON)) {
            return false
        }

        if (lookahead(1) === KtTokens.COLON && targetKeyword == null && at(KtTokens.IDENTIFIER)) {
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

    private fun parseAnnotationTarget(keyword: KtKeywordToken) {
        val message =
            "Expecting \"" + keyword.value + KtTokens.COLON.value + "\" prefix for " + keyword.value + " annotations"

        val marker = mark()

        if (!expect(keyword, message)) {
            marker.drop()
        } else {
            marker.done(KtNodeTypes.ANNOTATION_TARGET)
        }

        expect(KtTokens.COLON, message, IDENTIFIER_RBRACKET_LBRACKET_SET)
    }

    private fun atTargetKeyword(): KtKeywordToken? {
        for (target in ANNOTATION_TARGETS.getTypes()) {
            if (at(target)) return target as KtKeywordToken?
        }
        return null
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
        assert(
            _at(KtTokens.IDENTIFIER) ||  // We have "@ann" or "@:ann" or "@ :ann", but not "@ ann"
                    // (it's guaranteed that call sites do not allow the latter case)
                    (_at(KtTokens.AT) && (!isNextRawTokenCommentOrWhitespace || lookahead(1) === KtTokens.COLON))
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

        val whitespaceAfterAnnotation = KtTokens.WHITE_SPACE_OR_COMMENT_BIT_SET.contains(myBuilder.rawLookup(-1))
        val shouldBeParsedNextAsFunctionalType =
            at(KtTokens.LPAR) && whitespaceAfterAnnotation && mode.withSignificantWhitespaceBeforeArguments

        if (at(KtTokens.LPAR) && !shouldBeParsedNextAsFunctionalType) {
            myExpressionParsing.parseValueArgumentList()

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
        get() = KtTokens.WHITE_SPACE_OR_COMMENT_BIT_SET.contains(myBuilder.rawLookup(1))

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
        `object`: Boolean,
        nameParsingMode: NameParsingMode?,
        optionalBody: Boolean,
        enumClass: Boolean,
        expectKindKeyword: Boolean
    ): IElementType? {
        if (expectKindKeyword) {
            if (`object`) {
                assert(_at(KtTokens.OBJECT_KEYWORD))
            } else {
                assert(_atSet(CLASS_INTERFACE_SET))
            }
            advance() // CLASS_KEYWORD, INTERFACE_KEYWORD or OBJECT_KEYWORD
        } else {
            assert(enumClass) { "Currently classifiers without class/interface/object are only allowed for enums" }
            error("'class' keyword is expected after 'enum'")
        }

        if (nameParsingMode == NameParsingMode.REQUIRED) {
            expect(KtTokens.IDENTIFIER, "Name expected", CLASS_NAME_RECOVERY_SET)
        } else {
            assert(`object`) { "Must be an object to be nameless" }
            if (at(KtTokens.IDENTIFIER)) {
                if (nameParsingMode == NameParsingMode.PROHIBITED) {
                    errorAndAdvance("An object expression cannot bind a name")
                } else {
                    assert(nameParsingMode == NameParsingMode.ALLOWED)
                    advance()
                }
            }
        }

        val typeParametersDeclared = parseTypeParameterList(TYPE_PARAMETER_GT_RECOVERY_SET)

        val beforeConstructorModifiers = mark()
        val primaryConstructorMarker = mark()
        val hasConstructorModifiers = parseModifierList(TokenSet.EMPTY)

        // Some modifiers found, but no parentheses following: class has already ended, and we are looking at something else
        if (hasConstructorModifiers && !atSet(LPAR_LBRACE_COLON_CONSTRUCTOR_KEYWORD_SET)) {
            beforeConstructorModifiers.rollbackTo()
            return if (`object`) KtNodeTypes.OBJECT_DECLARATION else KtNodeTypes.CLASS
        }

        // We are still inside a class declaration
        beforeConstructorModifiers.drop()

        val hasConstructorKeyword = at(KtTokens.CONSTRUCTOR_KEYWORD)
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

        val whereMarker = OptionalMarker(`object`)
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

        return if (`object`) KtNodeTypes.OBJECT_DECLARATION else KtNodeTypes.CLASS
    }

    private fun parseClass(enumClass: Boolean, expectKindKeyword: Boolean): IElementType? {
        return parseClassOrObject(false, NameParsingMode.REQUIRED, true, enumClass, expectKindKeyword)
    }

    fun parseObject(nameParsingMode: NameParsingMode?, optionalBody: Boolean) {
        parseClassOrObject(true, nameParsingMode, optionalBody, enumClass = false, expectKindKeyword = true)
    }

    /*
     * enumClassBody
     *   : "{" enumEntries (";" members)? "}"
     *   ;
     */
    private fun parseEnumClassBody() {
        if (!at(KtTokens.LBRACE)) return

        val body = mark()
        myBuilder.enableNewlines()

        advance() // LBRACE

        if (!parseEnumEntries() && !at(KtTokens.RBRACE)) {
            error("Expecting ';' after the last enum entry or '}' to close enum class body")
        }
        parseMembers()
        expect(KtTokens.RBRACE, "Expecting '}' to close enum class body")

        myBuilder.restoreNewlinesState()
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

        if (!atSet(SOFT_KEYWORDS_AT_MEMBER_START) && at(KtTokens.IDENTIFIER)) {
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

                myExpressionParsing.parseValueArgumentList()
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

        myBuilder.enableNewlines()

        if (expect(KtTokens.LBRACE, "Expecting a class body")) {
            parseMembers()
            expect(KtTokens.RBRACE, "Missing '}")
        }

        myBuilder.restoreNewlinesState()

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
        parseModifierList(detector, TokenSet.EMPTY)

        val declType = parseMemberDeclarationRest(detector)

        if (declType == null) {
            errorWithRecovery("Expecting member declaration", TokenSet.EMPTY)
            decl.drop()
        } else {
            closeDeclarationWithCommentBinders(decl, declType, true)
        }
    }

    private fun parseMemberDeclarationRest(modifierDetector: ModifierDetector): IElementType? {
        var declType = parseCommonDeclaration(
            modifierDetector,
            if (modifierDetector.isCompanionDetected) NameParsingMode.ALLOWED else NameParsingMode.REQUIRED,
            DeclarationParsingMode.MEMBER_OR_TOPLEVEL
        )

        if (declType != null) return declType

        if (at(KtTokens.INIT_KEYWORD)) {
            advance() // init
            if (at(KtTokens.LBRACE)) {
                parseBlock()
            } else {
                mark().error("Expecting '{' after 'init'")
            }
            declType = KtNodeTypes.CLASS_INITIALIZER
        } else if (at(KtTokens.CONSTRUCTOR_KEYWORD)) {
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
     * constructorDelegationCall
     *   : "this" valueArguments
     *   : "super" valueArguments
     */
    private fun parseSecondaryConstructor() {
        assert(_at(KtTokens.CONSTRUCTOR_KEYWORD))

        advance() // CONSTRUCTOR_KEYWORD

        if (at(KtTokens.LPAR)) {
            parseValueParameterList(isFunctionTypeContents = false, typeRequired = true, recoverySet = VALUE_ARGS_RECOVERY_SET)
        } else {
            errorWithRecovery("Expecting '('", TokenSet.orSet(VALUE_ARGS_RECOVERY_SET, TokenSet.create(KtTokens.COLON)))
        }

        if (at(KtTokens.COLON)) {
            advance() // COLON

            val delegationCall = mark()

            if (at(KtTokens.THIS_KEYWORD) || at(KtTokens.SUPER_KEYWORD)) {
                parseThisOrSuper()
                myExpressionParsing.parseValueArgumentList()
            } else {
                error("Expecting a 'this' or 'super' constructor call")
                var beforeWrongDelegationCallee: PsiBuilder.Marker? = null
                if (!at(KtTokens.LPAR)) {
                    beforeWrongDelegationCallee = mark()
                    advance() // wrong delegation callee
                }
                myExpressionParsing.parseValueArgumentList()

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
        assert(_at(KtTokens.THIS_KEYWORD) || _at(KtTokens.SUPER_KEYWORD))
        val mark = mark()

        advance() // THIS_KEYWORD | SUPER_KEYWORD

        mark.done(KtNodeTypes.CONSTRUCTOR_DELEGATION_REFERENCE)
    }

    /*
     * typeAlias
     *   : modifiers "typealias" SimpleName typeParameters? "=" type
     *   ;
     */
    private fun parseTypeAlias(): IElementType? {
        assert(_at(KtTokens.TYPE_ALIAS_KEYWORD))

        advance() // TYPE_ALIAS_KEYWORD

        expect(KtTokens.IDENTIFIER, "Type name expected", LT_EQ_SEMICOLON_TOP_LEVEL_DECLARATION_FIRST_SET)

        parseTypeParameterList(TYPE_PARAMETER_GT_RECOVERY_SET)

        if (at(KtTokens.WHERE_KEYWORD)) {
            val error = mark()
            parseTypeConstraints()
            error.error("Type alias parameters can't have bounds")
        }

        expect(KtTokens.EQ, "Expecting '='", TOP_LEVEL_DECLARATION_FIRST_SEMICOLON_SET)

        parseTypeRef()

        consumeIf(KtTokens.SEMICOLON)

        return KtNodeTypes.TYPEALIAS
    }

    enum class DeclarationParsingMode(
        val destructuringAllowed: Boolean,
        val accessorsAllowed: Boolean,
        val canBeEnumUsedAsSoftKeyword: Boolean
    ) {
        MEMBER_OR_TOPLEVEL(false, true, true),
        LOCAL(true, false, false),
        SCRIPT_TOPLEVEL(true, true, false)
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
    fun parseProperty(mode: DeclarationParsingMode): IElementType {
        assert(at(KtTokens.VAL_KEYWORD) || at(KtTokens.VAR_KEYWORD))
        advance()

        val typeParametersDeclared = at(KtTokens.LT) && parseTypeParameterList(IDENTIFIER_EQ_COLON_SEMICOLON_SET)

        myBuilder.disableJoiningComplexTokens()

        val receiver = mark()
        val receiverTypeDeclared = parseReceiverType("property", PROPERTY_NAME_FOLLOW_SET)

        val multiDeclaration = at(KtTokens.LPAR)

        errorIf(receiver, multiDeclaration && receiverTypeDeclared, "Receiver type is not allowed on a destructuring declaration")

        val isNameOnTheNextLine = eol()
        val beforeName = mark()

        if (multiDeclaration) {
            val multiDecl = mark()
            parseMultiDeclarationName(PROPERTY_NAME_FOLLOW_SET, PROPERTY_NAME_FOLLOW_MULTI_DECLARATION_RECOVERY_SET)
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

        myBuilder.restoreJoiningComplexTokensState()

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
            beforeName.rollbackTo()
            error("Expecting property name or receiver type")
            return KtNodeTypes.PROPERTY
        }

        beforeName.drop()

        if (mode.accessorsAllowed) {
            // It's only needed for non-local properties, because in local ones:
            // "val a = 1; b" must not be an infix call of b on "val ...;"

            myBuilder.enableNewlines()
            val hasNewLineWithSemicolon = consumeIf(KtTokens.SEMICOLON) && myBuilder.newlineBeforeCurrentToken()
            myBuilder.restoreNewlinesState()

            if (!hasNewLineWithSemicolon) {
                val alreadyRead = PropertyComponentKind.Collector()
                var propertyComponentKind = parsePropertyComponent(alreadyRead)

                while (propertyComponentKind != null) {
                    alreadyRead.collect(propertyComponentKind)
                    propertyComponentKind = parsePropertyComponent(alreadyRead)
                }

                if (!atSet(EOL_OR_SEMICOLON_RBRACE_SET)) {
                    if (lastToken !== KtTokens.SEMICOLON) {
                        errorUntil(
                            "Property getter or setter expected",
                            TokenSet.orSet(DECLARATION_FIRST, TokenSet.create(KtTokens.EOL_OR_SEMICOLON, KtTokens.LBRACE, KtTokens.RBRACE))
                        )
                    }
                } else {
                    consumeIf(KtTokens.SEMICOLON)
                }
            }
        }

        return if (multiDeclaration) KtNodeTypes.DESTRUCTURING_DECLARATION else KtNodeTypes.PROPERTY
    }

    private fun parsePropertyDelegateOrAssignment(): Boolean {
        if (at(KtTokens.BY_KEYWORD)) {
            parsePropertyDelegate()
            return true
        } else if (at(KtTokens.EQ)) {
            advance() // EQ
            myExpressionParsing.parseExpression()
            return true
        }

        return false
    }

    /*
     * propertyDelegate
     *   : "by" expression
     *   ;
     */
    private fun parsePropertyDelegate() {
        assert(_at(KtTokens.BY_KEYWORD))
        val delegate = mark()
        advance() // BY_KEYWORD
        myExpressionParsing.parseExpression()
        delegate.done(KtNodeTypes.PROPERTY_DELEGATE)
    }

    /*
     * (SimpleName (":" type){","})
     */
    fun parseMultiDeclarationName(follow: TokenSet, recoverySet: TokenSet?) {
        // Parsing multi-name, e.g.
        //   val (a, b) = foo()
        myBuilder.disableNewlines()
        advance() // LPAR

        if (!atSet(follow)) {
            while (true) {
                if (at(KtTokens.COMMA)) {
                    errorAndAdvance("Expecting a name")
                } else if (at(KtTokens.RPAR)) { // For declaration similar to `val () = somethingCall()`
                    error("Expecting a name")
                    break
                }
                val property = mark()

                parseModifierList(COMMA_RPAR_COLON_EQ_SET)

                expect(KtTokens.IDENTIFIER, "Expecting a name", recoverySet)

                if (at(KtTokens.COLON)) {
                    advance() // COLON
                    parseTypeRef(follow)
                }
                property.done(KtNodeTypes.DESTRUCTURING_DECLARATION_ENTRY)

                if (!at(KtTokens.COMMA)) break
                advance() // COMMA
                if (at(KtTokens.RPAR)) break
            }
        }

        expect(KtTokens.RPAR, "Expecting ')'", follow)
        myBuilder.restoreNewlinesState()
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

        parseModifierList(TokenSet.EMPTY)
        val propertyComponentKind = if (at(KtTokens.GET_KEYWORD)) {
            PropertyComponentKind.GET
        } else if (at(KtTokens.SET_KEYWORD)) {
            PropertyComponentKind.SET
        } else if (at(KtTokens.FIELD_KEYWORD)) {
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
            if (!atSet(ACCESSOR_FIRST_OR_PROPERTY_END)) {
                errorUntil(
                    "Accessor body expected",
                    TokenSet.orSet(ACCESSOR_FIRST_OR_PROPERTY_END, TokenSet.create(KtTokens.LBRACE, KtTokens.LPAR, KtTokens.EQ))
                )
            } else {
                closeDeclarationWithCommentBinders(propertyComponent, KtNodeTypes.PROPERTY_ACCESSOR, true)
                return propertyComponentKind
            }
        }

        myBuilder.disableNewlines()

        if (propertyComponentKind != PropertyComponentKind.FIELD) {
            val parameterList = mark()
            expect(KtTokens.LPAR, "Expecting '('", RPAR_IDENTIFIER_COLON_LBRACE_EQ_SET)
            if (propertyComponentKind == PropertyComponentKind.SET) {
                val setterParameter = mark()
                parseModifierList(COMMA_COLON_RPAR_SET)
                expect(KtTokens.IDENTIFIER, "Expecting parameter name", RPAR_COLON_LBRACE_EQ_SET)

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
                errorUntil(
                    "Expecting ')'",
                    TokenSet.create(
                        KtTokens.RPAR,
                        KtTokens.COLON,
                        KtTokens.LBRACE,
                        KtTokens.RBRACE,
                        KtTokens.EQ,
                        KtTokens.EOL_OR_SEMICOLON
                    )
                )
            }
            if (at(KtTokens.RPAR)) {
                advance()
            }
            parameterList.done(KtNodeTypes.VALUE_PARAMETER_LIST)
        }
        myBuilder.restoreNewlinesState()

        if (at(KtTokens.COLON)) {
            advance()

            parseTypeRef()
        }

        if (propertyComponentKind != PropertyComponentKind.FIELD) {
            parseFunctionContract()
            parseFunctionBody()
        } else if (at(KtTokens.EQ)) {
            advance()
            myExpressionParsing.parseExpression()
            consumeIf(KtTokens.SEMICOLON)
        }

        if (propertyComponentKind == PropertyComponentKind.FIELD) {
            closeDeclarationWithCommentBinders(propertyComponent, KtNodeTypes.BACKING_FIELD, true)
        } else {
            closeDeclarationWithCommentBinders(propertyComponent, KtNodeTypes.PROPERTY_ACCESSOR, true)
        }

        return propertyComponentKind
    }

    private fun parseFunction(): IElementType? {
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
    fun parseFunction(failIfIdentifierExists: Boolean): IElementType? {
        assert(_at(KtTokens.FUN_KEYWORD))

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

        myBuilder.disableJoiningComplexTokens()

        val receiverFound = parseReceiverType("function", FUNCTION_NAME_FOLLOW_SET)

        if (at(KtTokens.IDENTIFIER) && failIfIdentifierExists) {
            myBuilder.restoreJoiningComplexTokensState()
            return null
        }

        // function as expression has no name
        parseFunctionOrPropertyName(
            receiverFound,
            "function",
            FUNCTION_NAME_FOLLOW_SET,
            FUNCTION_NAME_RECOVERY_SET,  /*nameRequired = */
            false
        )

        myBuilder.restoreJoiningComplexTokensState()

        if (at(KtTokens.LT)) {
            var error = mark()
            parseTypeParameterList(LPAR_VALUE_PARAMETERS_FOLLOW_SET)
            if (typeParameterListOccurred) {
                val finishIndex = myBuilder.rawTokenIndex()
                error.rollbackTo()
                error = mark()
                advance(finishIndex - myBuilder.rawTokenIndex())
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
    private fun parseReceiverType(title: String?, nameFollow: TokenSet?): Boolean {
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

        createTruncatedBuilder(lastDot)!!.parseTypeRefWithoutIntersections()

        if (atSet(RECEIVER_TYPE_TERMINATORS)) {
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
        title: String?,
        nameFollow: TokenSet,
        recoverySet: TokenSet?,
        nameRequired: Boolean
    ) {
        if (!nameRequired && atSet(nameFollow)) return  // no name


        if (expect(KtTokens.IDENTIFIER)) {
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
        if (at(KtTokens.LBRACE)) {
            parseBlock()
        } else if (at(KtTokens.EQ)) {
            advance() // EQ
            myExpressionParsing.parseExpression()
            consumeIf(KtTokens.SEMICOLON)
        } else {
            error("Expecting function body")
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

        myBuilder.enableNewlines()

        val hasOpeningBrace = expect(KtTokens.LBRACE, "Expecting '{' to open a block")
        val canCollapse = collapse && hasOpeningBrace && isLazy

        if (canCollapse) {
            advanceBalancedBlock()
        } else {
            myExpressionParsing.parseStatements()
            expect(KtTokens.RBRACE, "Expecting '}'")
        }

        myBuilder.restoreNewlinesState()

        if (canCollapse) {
            lazyBlock.collapse(KtNodeTypes.BLOCK)
        } else {
            lazyBlock.done(KtNodeTypes.BLOCK)
        }
    }

    fun advanceBalancedBlock() {
        var braceCount = 1
        while (!eof()) {
            if (_at(KtTokens.LBRACE)) {
                braceCount++
            } else if (_at(KtTokens.RBRACE)) {
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

        if (at(KtTokens.BY_KEYWORD)) {
            reference.drop()
            advance() // BY_KEYWORD
            createForByClause(myBuilder, isLazy).myExpressionParsing.parseExpression()
            delegator.done(KtNodeTypes.DELEGATED_SUPER_TYPE_ENTRY)
        } else if (at(KtTokens.LPAR)) {
            reference.done(KtNodeTypes.CONSTRUCTOR_CALLEE)
            myExpressionParsing.parseValueArgumentList()
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
    private fun parseTypeParameterList(recoverySet: TokenSet?): Boolean {
        var result = false
        if (at(KtTokens.LT)) {
            val list = mark()

            myBuilder.disableNewlines()
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
            myBuilder.restoreNewlinesState()
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
        if (at(KtTokens.WHERE_KEYWORD)) {
            parseTypeConstraintList()
            return true
        }
        return false
    }

    /*
     * typeConstraint{","}
     */
    private fun parseTypeConstraintList() {
        assert(_at(KtTokens.WHERE_KEYWORD))

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
        if (expect(KtTokens.IDENTIFIER, "Expecting type parameter name", COLON_COMMA_LBRACE_RBRACE_TYPE_REF_FIRST_SET)) {
            reference.done(KtNodeTypes.REFERENCE_EXPRESSION)
        } else {
            reference.drop()
        }

        expect(KtTokens.COLON, "Expecting ':' before the upper bound", LBRACE_RBRACE_TYPE_REF_FIRST_SET)

        parseTypeRef()

        constraint.done(KtNodeTypes.TYPE_CONSTRAINT)
    }

    private fun parseFunctionContract(): Boolean {
        if (at(KtTokens.CONTRACT_KEYWORD)) {
            myExpressionParsing.parseContractDescriptionBlock()
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
        if (atSet(TYPE_PARAMETER_GT_RECOVERY_SET)) {
            error("Type parameter declaration expected")
            return
        }

        val mark = mark()

        parseModifierList(GT_COMMA_COLON_SET)

        expect(KtTokens.IDENTIFIER, "Type parameter name expected", TokenSet.EMPTY)

        if (at(KtTokens.COLON)) {
            advance() // COLON
            parseTypeRef()
        }

        mark.done(KtNodeTypes.TYPE_PARAMETER)
    }

    fun parseTypeRefWithoutIntersections() {
        parseTypeRef(TokenSet.EMPTY,  /* allowSimpleIntersectionTypes */false)
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
    @JvmOverloads
    fun parseTypeRef(extraRecoverySet: TokenSet? = TokenSet.EMPTY) {
        parseTypeRef(extraRecoverySet, allowSimpleIntersectionTypes = true)
    }

    private fun parseTypeRef(extraRecoverySet: TokenSet?, allowSimpleIntersectionTypes: Boolean) {
        val typeRefMarker = parseTypeRefContents(extraRecoverySet, allowSimpleIntersectionTypes)
        typeRefMarker.done(KtNodeTypes.TYPE_REFERENCE)
    }

    // The extraRecoverySet is needed for the foo(bar<x, 1, y>(z)) case, to tell whether we should stop
    // on expression-indicating symbols or not
    private fun parseTypeRefContents(extraRecoverySet: TokenSet?, allowSimpleIntersectionTypes: Boolean): PsiBuilder.Marker {
        val typeRefMarker = mark()

        parseTypeModifierList()

        val lookahead = lookahead(1)
        val lookahead2 = lookahead(2)
        var typeBeforeDot = true
        val withContextReceiver = at(KtTokens.CONTEXT_KEYWORD) && lookahead === KtTokens.LPAR
        var wasFunctionTypeParsed = false

        val contextReceiversStart = mark()

        if (withContextReceiver) {
            parseContextReceiverList(true)
        }

        var typeElementMarker = mark()

        if (at(KtTokens.IDENTIFIER) && !(lookahead === KtTokens.DOT && lookahead2 === KtTokens.IDENTIFIER) && lookahead !== KtTokens.LT && at(
                KtTokens.DYNAMIC_KEYWORD
            )
        ) {
            val dynamicType = mark()
            advance() // DYNAMIC_KEYWORD
            dynamicType.done(KtNodeTypes.DYNAMIC_TYPE)
        } else if (at(KtTokens.IDENTIFIER) || at(KtTokens.PACKAGE_KEYWORD) || atParenthesizedMutableForPlatformTypes(0)) {
            parseUserType()
        } else if (at(KtTokens.LPAR)) {
            val functionOrParenthesizedType = mark()

            // This may be a function parameter list or just a parenthesized type
            advance() // LPAR
            parseTypeRefContents(
                TokenSet.EMPTY,  /* allowSimpleIntersectionTypes */
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
        } else {
            errorWithRecovery(
                "Type expected",
                TokenSet.orSet(
                    TOP_LEVEL_DECLARATION_FIRST,
                    TokenSet.create(
                        KtTokens.EQ,
                        KtTokens.COMMA,
                        KtTokens.GT,
                        KtTokens.RBRACKET,
                        KtTokens.DOT,
                        KtTokens.RPAR,
                        KtTokens.RBRACE,
                        KtTokens.LBRACE,
                        KtTokens.SEMICOLON
                    ),
                    extraRecoverySet
                )
            )
            typeBeforeDot = false
        }

        // Disabling token merge is required for cases like
        //    Int?.(Foo) -> Bar
        myBuilder.disableJoiningComplexTokens()
        typeElementMarker = parseNullableTypeSuffix(typeElementMarker)
        myBuilder.restoreJoiningComplexTokensState()

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
            errorWithRecovery(
                "Function type expected expected",
                TokenSet.orSet(
                    TOP_LEVEL_DECLARATION_FIRST,
                    TokenSet.create(
                        KtTokens.EQ,
                        KtTokens.COMMA,
                        KtTokens.GT,
                        KtTokens.RBRACKET,
                        KtTokens.DOT,
                        KtTokens.RPAR,
                        KtTokens.RBRACE,
                        KtTokens.LBRACE,
                        KtTokens.SEMICOLON
                    ),
                    extraRecoverySet
                )
            )
        }

        typeElementMarker.drop()
        contextReceiversStart.drop()
        return typeRefMarker
    }

    private fun parseNullableTypeSuffix(typeElementMarker: PsiBuilder.Marker): PsiBuilder.Marker {
        // ?: is joined regardless of joining state
        var typeElementMarker = typeElementMarker
        while (at(KtTokens.QUEST) && myBuilder.rawLookup(1) !== KtTokens.COLON) {
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
            expect(KtTokens.DOT, "Expecting '.'", TokenSet.create(KtTokens.IDENTIFIER, KtTokens.LBRACE, KtTokens.RBRACE))
        }

        var reference = mark()
        while (true) {
            recoverOnParenthesizedWordForPlatformTypes(0, "Mutable", true)

            if (expect(
                    KtTokens.IDENTIFIER, "Expecting type name",
                    TokenSet.orSet(KotlinExpressionParsing.EXPRESSION_FIRST, KotlinExpressionParsing.EXPRESSION_FOLLOW, DECLARATION_FIRST)
                )
            ) {
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
        if (lookahead(offset) === KtTokens.LPAR && lookahead(offset + 1) === KtTokens.IDENTIFIER && lookahead(offset + 2) === KtTokens.RPAR && lookahead(
                offset + 3
            ) === KtTokens.IDENTIFIER
        ) {
            val error = mark()

            advance(offset)

            advance() // LPAR
            if (word != myBuilder.tokenText) {
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

        tryParseTypeArgumentList(TokenSet.EMPTY)

        list.done(KtNodeTypes.TYPE_ARGUMENT_LIST)
    }

    fun tryParseTypeArgumentList(extraRecoverySet: TokenSet?): Boolean {
        myBuilder.disableNewlines()
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
        myBuilder.restoreNewlinesState()
        return atGT
    }

    /*
     * functionType
     *   : (type ".")? "(" parameter{","}? ")" "->" type?
     *   ;
     */
    private fun parseFunctionType(functionType: PsiBuilder.Marker?) {
        parseFunctionTypeContents(functionType)!!.done(KtNodeTypes.FUNCTION_TYPE)
    }

    private fun parseFunctionTypeContents(functionType: PsiBuilder.Marker?): PsiBuilder.Marker? {
        assert(_at(KtTokens.LPAR)) { tt()!! }

        parseValueParameterList(isFunctionTypeContents = true, typeRequired = true, recoverySet = TokenSet.EMPTY)

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
    private fun parseValueParameterList(isFunctionTypeContents: Boolean, typeRequired: Boolean, recoverySet: TokenSet) {
        assert(_at(KtTokens.LPAR))
        val parameters = mark()

        myBuilder.disableNewlines()

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
        }

        myBuilder.restoreNewlinesState()

        parameters.done(KtNodeTypes.VALUE_PARAMETER_LIST)
    }

    private fun valueParameterLoop(inFunctionTypeContext: Boolean, recoverySet: TokenSet, parseParameter: Runnable) {
        advance() // LPAR

        if (!at(KtTokens.RPAR) && !atSet(recoverySet)) {
            while (true) {
                if (at(KtTokens.COMMA)) {
                    errorAndAdvance("Expecting a parameter declaration")
                } else if (at(KtTokens.RPAR)) {
                    break
                }

                parseParameter.run()

                if (at(KtTokens.COMMA)) {
                    advance() // COMMA
                } else if (at(KtTokens.COLON)) {
                    // recovery for the case "fun bar(x: Array<Int> : Int)" when we've just parsed "x: Array<Int>"
                    // error should be reported in the `parseValueParameter` call
                    continue
                } else {
                    if (!at(KtTokens.RPAR)) error("Expecting comma or ')'")
                    if (!atSet(if (inFunctionTypeContext) LAMBDA_VALUE_PARAMETER_FIRST else VALUE_PARAMETER_FIRST)) break
                }
            }
        }

        expect(KtTokens.RPAR, "Expecting ')'", recoverySet)
    }

    /*
     * functionParameter
     *   : modifiers ("val" | "var")? parameter ("=" element)?
     *   ;
     */
    private fun tryParseValueParameter(typeRequired: Boolean): Boolean {
        return parseValueParameter(true, typeRequired)
    }

    fun parseValueParameter(typeRequired: Boolean) {
        parseValueParameter(false, typeRequired)
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

        // Recovery for the case 'fun foo(Array<String>) {}'
        // Recovery for the case 'fun foo(: Int) {}'
        if ((at(KtTokens.IDENTIFIER) && lookahead(1) === KtTokens.LT) || at(KtTokens.COLON)) {
            error("Parameter name expected")
            if (at(KtTokens.COLON)) {
                // We keep noErrors == true so that unnamed parameters starting with ":" are not rolled back during parsing of functional types
                advance() // COLON
            } else {
                noErrors = false
            }
            parseTypeRef()
        } else {
            expect(KtTokens.IDENTIFIER, "Parameter name expected", PARAMETER_NAME_RECOVERY_SET)

            if (at(KtTokens.COLON)) {
                advance() // COLON

                if (at(KtTokens.IDENTIFIER) && lookahead(1) === KtTokens.COLON) {
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
            myExpressionParsing.parseExpression()
        }

        return noErrors
    }

    override fun create(builder: SemanticWhitespaceAwarePsiBuilder): KotlinParsing {
        return createForTopLevel(builder)
    }

    /*package*/
    class ModifierDetector : Consumer<IElementType?> {
        var isEnumDetected: Boolean = false
            private set
        var isCompanionDetected: Boolean = false
            private set

        override fun consume(item: IElementType?) {
            if (item === KtTokens.ENUM_KEYWORD) {
                this.isEnumDetected = true
            } else if (item === KtTokens.COMPANION_KEYWORD) {
                this.isCompanionDetected = true
            }
        }
    }

    enum class AnnotationParsingMode(
        val isFileAnnotationParsingMode: Boolean,
        val allowAnnotations: Boolean,
        val allowContextList: Boolean,
        val typeContext: Boolean,
        val withSignificantWhitespaceBeforeArguments: Boolean
    ) {
        DEFAULT(false, true, true, false, false),
        FILE_ANNOTATIONS_BEFORE_PACKAGE(true, true, false, false, false),
        FILE_ANNOTATIONS_WHEN_PACKAGE_OMITTED(true, true, false, false, false),
        TYPE_CONTEXT(false, true, false, true, false),
        WITH_SIGNIFICANT_WHITESPACE_BEFORE_ARGUMENTS(false, true, true, true, true),
        WITH_SIGNIFICANT_WHITESPACE_BEFORE_ARGUMENTS_NO_CONTEXT(false, true, false, true, true),
        NO_ANNOTATIONS_NO_CONTEXT(false, false, false, false, false)
    }
}
