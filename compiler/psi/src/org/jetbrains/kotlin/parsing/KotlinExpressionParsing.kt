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

import com.google.common.collect.ImmutableMap
import com.intellij.lang.PsiBuilder
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.lexer.KtTokens
import java.util.*

open class KotlinExpressionParsing @JvmOverloads constructor(
    builder: SemanticWhitespaceAwarePsiBuilder,
    private val myKotlinParsing: KotlinParsing,
    isLazy: Boolean = true
) : AbstractKotlinParsing(builder, isLazy) {
    enum class Precedence(vararg operations: IElementType?) {
        POSTFIX(
            KtTokens.PLUSPLUS, KtTokens.MINUSMINUS, KtTokens.EXCLEXCL,
            KtTokens.DOT, KtTokens.SAFE_ACCESS
        ),  // typeArguments? valueArguments : typeArguments : arrayAccess

        PREFIX(KtTokens.MINUS, KtTokens.PLUS, KtTokens.MINUSMINUS, KtTokens.PLUSPLUS, KtTokens.EXCL) {
            // annotations
            override fun parseHigherPrecedence(parser: KotlinExpressionParsing) {
                throw IllegalStateException("Don't call this method")
            }
        },

        AS(KtTokens.AS_KEYWORD, KtTokens.`AS_SAFE`) {
            override fun parseRightHandSide(operation: IElementType?, parser: KotlinExpressionParsing): IElementType {
                parser.myKotlinParsing.parseTypeRefWithoutIntersections()
                return KtNodeTypes.BINARY_WITH_TYPE
            }

            override fun parseHigherPrecedence(parser: KotlinExpressionParsing) {
                parser.parsePrefixExpression()
            }
        },

        MULTIPLICATIVE(KtTokens.MUL, KtTokens.DIV, KtTokens.PERC),
        ADDITIVE(KtTokens.PLUS, KtTokens.MINUS),
        RANGE(KtTokens.RANGE, KtTokens.RANGE_UNTIL),
        SIMPLE_NAME(KtTokens.IDENTIFIER),
        ELVIS(KtTokens.ELVIS),
        IN_OR_IS(KtTokens.IN_KEYWORD, KtTokens.NOT_IN, KtTokens.IS_KEYWORD, KtTokens.NOT_IS) {
            override fun parseRightHandSide(operation: IElementType?, parser: KotlinExpressionParsing): IElementType {
                if (operation === KtTokens.IS_KEYWORD || operation === KtTokens.NOT_IS) {
                    parser.myKotlinParsing.parseTypeRefWithoutIntersections()
                    return KtNodeTypes.IS_EXPRESSION
                }

                return super.parseRightHandSide(operation, parser)
            }
        },
        COMPARISON(KtTokens.LT, KtTokens.GT, KtTokens.LTEQ, KtTokens.GTEQ),
        EQUALITY(KtTokens.EQEQ, KtTokens.EXCLEQ, KtTokens.EQEQEQ, KtTokens.EXCLEQEQEQ),
        CONJUNCTION(KtTokens.ANDAND),
        DISJUNCTION(KtTokens.OROR),

        //        ARROW(KtTokens.ARROW),
        ASSIGNMENT(KtTokens.EQ, KtTokens.PLUSEQ, KtTokens.MINUSEQ, KtTokens.MULTEQ, KtTokens.DIVEQ, KtTokens.PERCEQ),
        ;

        private var higher: Precedence? = null
        @JvmField
        val operations: TokenSet = TokenSet.create(*operations)

        open fun parseHigherPrecedence(parser: KotlinExpressionParsing) {
            checkNotNull(higher)
            parser.parseBinaryExpression(higher!!)
        }

        /**
         *
         * @param operation the operation sign (e.g. PLUS or IS)
         * @param parser the parser object
         * @return node type of the result
         */
        open fun parseRightHandSide(operation: IElementType?, parser: KotlinExpressionParsing): IElementType {
            parseHigherPrecedence(parser)
            return KtNodeTypes.BINARY_EXPRESSION
        }

        companion object {
            init {
                val values = entries.toTypedArray()
                for (precedence in values) {
                    val ordinal = precedence.ordinal
                    precedence.higher = if (ordinal > 0) values[ordinal - 1] else null
                }
            }
        }
    }

    /*
     * element
     *   : annotations element
     *   : "(" element ")" // see tupleLiteral
     *   : literalConstant
     *   : functionLiteral
     *   : tupleLiteral
     *   : "null"
     *   : "this" ("<" type ">")?
     *   : expressionWithPrecedences
     *   : if
     *   : try
     *   : "typeof" "(" element ")"
     *   : "new" constructorInvocation
     *   : objectLiteral
     *   : declaration
     *   : jump
     *   : loop
     *   // block is syntactically equivalent to a functionLiteral with no parameters
     *   ;
     */
    fun parseExpression() {
        if (!atSet(EXPRESSION_FIRST)) {
            error("Expecting an expression")
            return
        }

        parseBinaryExpression(Precedence.ASSIGNMENT)
    }

    /*
     * element (operation element)*
     *
     * see the precedence table
     */
    private fun parseBinaryExpression(precedence: Precedence) {
        var expression = mark()

        precedence.parseHigherPrecedence(this)

        while (!interruptedWithNewLine() && atSet(precedence.operations)) {
            val operation = tt()

            parseOperationReference()

            val resultType = precedence.parseRightHandSide(operation, this)
            expression.done(resultType)
            expression = expression.precede()
        }

        expression.drop()
    }

    /*
     * label prefixExpression
     */
    private fun parseLabeledExpression() {
        val expression = mark()
        parseLabelDefinition()
        parsePrefixExpression()
        expression.done(KtNodeTypes.LABELED_EXPRESSION)
    }

    /*
     * operation? prefixExpression
     */
    private fun parsePrefixExpression() {
        if (at(KtTokens.AT)) {
            if (!parseLocalDeclaration(rollbackIfDefinitelyNotExpression = false, isScriptTopLevel = false)) {
                val expression = mark()
                myKotlinParsing.parseAnnotations(KotlinParsing.AnnotationParsingMode.DEFAULT)
                parsePrefixExpression()
                expression.done(KtNodeTypes.ANNOTATED_EXPRESSION)
            }
        } else {
            myBuilder.disableJoiningComplexTokens()
            if (this.isAtLabelDefinitionOrMissingIdentifier) {
                myBuilder.restoreJoiningComplexTokensState()
                parseLabeledExpression()
            } else if (atSet(Precedence.PREFIX.operations)) {
                val expression = mark()

                parseOperationReference()

                myBuilder.restoreJoiningComplexTokensState()

                parsePrefixExpression()
                expression.done(KtNodeTypes.PREFIX_EXPRESSION)
            } else {
                myBuilder.restoreJoiningComplexTokensState()
                parsePostfixExpression()
            }
        }
    }

    /*
     * doubleColonSuffix
     *   : "::" SimpleName typeArguments?
     *   ;
     */
    private fun parseDoubleColonSuffix(expression: PsiBuilder.Marker): Boolean {
        if (!at(KtTokens.COLONCOLON)) return false

        advance() // COLONCOLON

        if (at(KtTokens.CLASS_KEYWORD)) {
            advance() // CLASS_KEYWORD

            expression.done(KtNodeTypes.CLASS_LITERAL_EXPRESSION)
            return true
        }

        parseSimpleNameExpression()

        if (at(KtTokens.LT)) {
            val typeArgumentList = mark()
            if (myKotlinParsing.tryParseTypeArgumentList(TYPE_ARGUMENT_LIST_STOPPERS)) {
                typeArgumentList.error("Type arguments are not allowed")
            } else {
                typeArgumentList.rollbackTo()
            }
        }

        if (at(KtTokens.LPAR) && !myBuilder.newlineBeforeCurrentToken()) {
            val lpar = mark()
            parseCallSuffix()
            lpar.error("This syntax is reserved for future use; to call a reference, enclose it in parentheses: (foo::bar)(args)")
        }

        expression.done(KtNodeTypes.CALLABLE_REFERENCE_EXPRESSION)
        return true
    }

    private fun skipQuestionMarksBeforeDoubleColon() {
        if (at(KtTokens.QUEST)) {
            var k = 1
            while (lookahead(k) === KtTokens.QUEST) k++
            if (lookahead(k) === KtTokens.COLONCOLON) {
                while (k > 0) {
                    advance() // QUEST
                    k--
                }
            }
        }
    }

    /*
     * postfixUnaryExpression
     *   : atomicExpression postfixUnaryOperation*
     *   ;
     *
     * postfixUnaryOperation
     *   : "++" : "--" : "!!"
     *   : typeArguments? valueArguments (getEntryPoint? functionLiteral)
     *   : typeArguments (getEntryPoint? functionLiteral)
     *   : arrayAccess
     *   : memberAccessOperation postfixUnaryExpression // TODO: Review
     *   ;
     */
    private fun parsePostfixExpression() {
        var expression = mark()

        var firstExpressionParsed = if (at(KtTokens.COLONCOLON)) parseDoubleColonSuffix(mark()) else parseAtomicExpression()

        while (true) {
            if (interruptedWithNewLine()) {
                break
            } else if (at(KtTokens.LBRACKET)) {
                parseArrayAccess()
                expression.done(KtNodeTypes.ARRAY_ACCESS_EXPRESSION)
            } else if (parseCallSuffix()) {
                expression.done(KtNodeTypes.CALL_EXPRESSION)
            } else if (at(KtTokens.DOT) || at(KtTokens.SAFE_ACCESS)) {
                val expressionType = if (at(KtTokens.DOT)) KtNodeTypes.DOT_QUALIFIED_EXPRESSION else KtNodeTypes.SAFE_ACCESS_EXPRESSION
                advance() // DOT or SAFE_ACCESS

                if (!firstExpressionParsed) {
                    expression.drop()
                    expression = mark()
                    firstExpressionParsed = parseAtomicExpression()
                    continue
                }

                parseSelectorCallExpression()

                expression.done(expressionType)
            } else if (atSet(Precedence.POSTFIX.operations)) {
                parseOperationReference()
                expression.done(KtNodeTypes.POSTFIX_EXPRESSION)
            } else {
                skipQuestionMarksBeforeDoubleColon()
                if (!parseDoubleColonSuffix(expression)) {
                    break
                }
            }
            expression = expression.precede()
        }
        expression.drop()
    }

    /*
     * callSuffix
     *   : typeArguments? valueArguments annotatedLambda
     *   : typeArguments annotatedLambda
     *   ;
     */
    private fun parseCallSuffix(): Boolean {
        if (parseCallWithClosure()) {
            // do nothing
        } else if (at(KtTokens.LPAR)) {
            parseValueArgumentList()
            parseCallWithClosure()
        } else if (at(KtTokens.LT)) {
            val typeArgumentList = mark()
            if (myKotlinParsing.tryParseTypeArgumentList(TYPE_ARGUMENT_LIST_STOPPERS)) {
                typeArgumentList.done(KtNodeTypes.TYPE_ARGUMENT_LIST)
                if (!myBuilder.newlineBeforeCurrentToken() && at(KtTokens.LPAR)) parseValueArgumentList()
                parseCallWithClosure()
            } else {
                typeArgumentList.rollbackTo()
                return false
            }
        } else {
            return false
        }

        return true
    }

    /*
     * atomicExpression typeParameters? valueParameters? functionLiteral*
     */
    private fun parseSelectorCallExpression() {
        val mark = mark()
        parseAtomicExpression()
        if (!myBuilder.newlineBeforeCurrentToken() && parseCallSuffix()) {
            mark.done(KtNodeTypes.CALL_EXPRESSION)
        } else {
            mark.drop()
        }
    }

    private fun parseOperationReference() {
        val operationReference = mark()
        advance() // operation
        operationReference.done(KtNodeTypes.OPERATION_REFERENCE)
    }

    /*
     * annotatedLambda*
     */
    protected open fun parseCallWithClosure(): Boolean {
        var success = false

        while (true) {
            val argument = mark()

            if (!parseAnnotatedLambda( /* preferBlock = */false)) {
                argument.drop()
                break
            }

            argument.done(KtNodeTypes.LAMBDA_ARGUMENT)
            success = true
        }

        return success
    }

    /*
     * annotatedLambda
     *  : ("@" annotationEntry)* labelDefinition? functionLiteral
     */
    private fun parseAnnotatedLambda(preferBlock: Boolean): Boolean {
        val annotated = mark()

        val wereAnnotations = myKotlinParsing.parseAnnotations(KotlinParsing.AnnotationParsingMode.DEFAULT)
        val labeled = mark()

        val wasLabel = this.isAtLabelDefinitionOrMissingIdentifier
        if (wasLabel) {
            parseLabelDefinition()
        }

        if (!at(KtTokens.LBRACE)) {
            annotated.rollbackTo()
            return false
        }

        parseFunctionLiteral(preferBlock,  /* collapse = */true)

        doneOrDrop(labeled, KtNodeTypes.LABELED_EXPRESSION, wasLabel)
        doneOrDrop(annotated, KtNodeTypes.ANNOTATED_EXPRESSION, wereAnnotations)

        return true
    }

    val isAtLabelDefinitionOrMissingIdentifier: Boolean
        get() = (at(KtTokens.IDENTIFIER) && myBuilder.rawLookup(1) === KtTokens.AT) || at(
            KtTokens.AT
        )

    /*
    * atomicExpression
    *   : "this" label?
    *   : "super" ("<" type ">")? label?
    *   : objectLiteral
    *   : jump
    *   : if
    *   : when
    *   : try
    *   : loop
    *   : literalConstant
    *   : functionLiteral
    *   : declaration
    *   : SimpleName
    *   : collectionLiteral
    *   ;
    */
    private fun parseAtomicExpression(): Boolean {
        var ok = true

        when (tokenId) {
            KtTokens.LPAR_Id -> parseParenthesizedExpression()
            KtTokens.LBRACKET_Id -> parseCollectionLiteralExpression()
            KtTokens.THIS_KEYWORD_Id -> parseThisExpression()
            KtTokens.SUPER_KEYWORD_Id -> parseSuperExpression()
            KtTokens.OBJECT_KEYWORD_Id -> parseObjectLiteral()
            KtTokens.THROW_KEYWORD_Id -> parseThrow()
            KtTokens.RETURN_KEYWORD_Id -> parseReturn()
            KtTokens.CONTINUE_KEYWORD_Id -> parseJump(KtNodeTypes.CONTINUE)
            KtTokens.BREAK_KEYWORD_Id -> parseJump(KtNodeTypes.BREAK)
            KtTokens.IF_KEYWORD_Id -> parseIf()
            KtTokens.WHEN_KEYWORD_Id -> parseWhen()
            KtTokens.TRY_KEYWORD_Id -> parseTry()
            KtTokens.FOR_KEYWORD_Id -> parseFor()
            KtTokens.WHILE_KEYWORD_Id -> parseWhile()
            KtTokens.DO_KEYWORD_Id -> parseDoWhile()
            KtTokens.IDENTIFIER_Id -> {
                // Try to parse anonymous function with context parameters
                if (at(KtTokens.CONTEXT_KEYWORD) && lookahead(1) === KtTokens.LPAR) {
                    if (parseLocalDeclaration(rollbackIfDefinitelyNotExpression = true, isScriptTopLevel = false)) {
                        return true
                    } else {
                        at(KtTokens.IDENTIFIER)
                    }
                }

                parseSimpleNameExpression()
            }
            KtTokens.LBRACE_Id -> parseFunctionLiteral()
            KtTokens.INTERPOLATION_PREFIX_Id, KtTokens.OPEN_QUOTE_Id -> parseStringTemplate()
            KtTokens.TRUE_KEYWORD_Id, KtTokens.FALSE_KEYWORD_Id -> parseOneTokenExpression(KtNodeTypes.BOOLEAN_CONSTANT)
            KtTokens.INTEGER_LITERAL_Id -> parseOneTokenExpression(KtNodeTypes.INTEGER_CONSTANT)
            KtTokens.CHARACTER_LITERAL_Id -> parseOneTokenExpression(KtNodeTypes.CHARACTER_CONSTANT)
            KtTokens.FLOAT_LITERAL_Id -> parseOneTokenExpression(KtNodeTypes.FLOAT_CONSTANT)
            KtTokens.NULL_KEYWORD_Id -> parseOneTokenExpression(KtNodeTypes.NULL)
            KtTokens.CLASS_KEYWORD_Id, KtTokens.INTERFACE_KEYWORD_Id, KtTokens.FUN_KEYWORD_Id, KtTokens.VAL_KEYWORD_Id, KtTokens.VAR_KEYWORD_Id, KtTokens.TYPE_ALIAS_KEYWORD_Id -> if (!parseLocalDeclaration( /* rollbackIfDefinitelyNotExpression = */
                                                                                                                                                                                                               myBuilder.newlineBeforeCurrentToken(),
                                                                                                                                                                                                               false
                )
            ) {
                ok = false
            }
            else -> ok = false
        }

        if (!ok) {
            // TODO: better recovery if FIRST(element) did not match
            errorWithRecovery("Expecting an element", TokenSet.orSet(EXPRESSION_FOLLOW, TokenSet.create(KtTokens.LONG_TEMPLATE_ENTRY_END)))
        }

        return ok
    }

    /*
     * stringTemplate
     *   : INTERPOLATION_PREFIX OPEN_QUOTE stringTemplateElement* CLOSING_QUOTE
     *   ;
     */
    private fun parseStringTemplate() {
        assert(_at(KtTokens.INTERPOLATION_PREFIX) || _at(KtTokens.OPEN_QUOTE))

        val template = mark()

        if (at(KtTokens.INTERPOLATION_PREFIX)) {
            advance() // INTERPOLATION_PREFIX
        }

        assert(_at(KtTokens.OPEN_QUOTE))
        advance() // OPEN_QUOTE

        while (!eof()) {
            if (at(KtTokens.CLOSING_QUOTE) || at(KtTokens.DANGLING_NEWLINE)) {
                break
            }
            parseStringTemplateElement()
        }

        if (at(KtTokens.DANGLING_NEWLINE)) {
            errorAndAdvance("Expecting '\"'")
        } else {
            expect(KtTokens.CLOSING_QUOTE, "Expecting '\"'")
        }
        template.done(KtNodeTypes.STRING_TEMPLATE)
    }

    /*
     * stringTemplateElement
     *   : RegularStringPart
     *   : ShortTemplateEntrySTART (SimpleName | "this")
     *   : EscapeSequence
     *   : longTemplate
     *   ;
     *
     * longTemplate
     *   : "${" expression "}"
     *   ;
     */
    private fun parseStringTemplateElement() {
        if (at(KtTokens.REGULAR_STRING_PART)) {
            val mark = mark()
            advance() // REGULAR_STRING_PART
            mark.done(KtNodeTypes.LITERAL_STRING_TEMPLATE_ENTRY)
        } else if (at(KtTokens.ESCAPE_SEQUENCE)) {
            val mark = mark()
            advance() // ESCAPE_SEQUENCE
            mark.done(KtNodeTypes.ESCAPE_STRING_TEMPLATE_ENTRY)
        } else if (at(KtTokens.SHORT_TEMPLATE_ENTRY_START)) {
            val entry = mark()
            advance() // SHORT_TEMPLATE_ENTRY_START

            if (at(KtTokens.THIS_KEYWORD)) {
                val thisExpression = mark()
                val reference = mark()
                advance() // THIS_KEYWORD
                reference.done(KtNodeTypes.REFERENCE_EXPRESSION)
                thisExpression.done(KtNodeTypes.THIS_EXPRESSION)
            } else {
                val keyword: KtToken? = KEYWORD_TEXTS.get(myBuilder.tokenText)
                if (keyword != null) {
                    myBuilder.remapCurrentToken(keyword)
                    errorAndAdvance("Keyword cannot be used as a reference")
                } else {
                    val reference = mark()
                    expect(KtTokens.IDENTIFIER, "Expecting a name")
                    reference.done(KtNodeTypes.REFERENCE_EXPRESSION)
                }
            }

            entry.done(KtNodeTypes.SHORT_STRING_TEMPLATE_ENTRY)
        } else if (at(KtTokens.LONG_TEMPLATE_ENTRY_START)) {
            val longTemplateEntry = mark()

            advance() // LONG_TEMPLATE_ENTRY_START

            while (!eof()) {
                val offset = myBuilder.currentOffset

                parseExpression()

                if (_at(KtTokens.LONG_TEMPLATE_ENTRY_END)) {
                    advance()
                    break
                } else {
                    error("Expecting '}'")
                    if (offset == myBuilder.currentOffset) {
                        // Prevent hang if can't advance with parseExpression()
                        advance()
                    }
                }
            }

            longTemplateEntry.done(KtNodeTypes.LONG_STRING_TEMPLATE_ENTRY)
        } else {
            errorAndAdvance("Unexpected token in a string template")
        }
    }

    /*
     * when
     *   : "when" ("(" (modifiers "val" SimpleName "=")? element ")")? "{"
     *         whenEntry*
     *     "}"
     *   ;
     */
    private fun parseWhen() {
        assert(_at(KtTokens.WHEN_KEYWORD))

        val `when` = mark()

        advance() // WHEN_KEYWORD

        // Parse condition
        myBuilder.disableNewlines()
        if (at(KtTokens.LPAR)) {
            advanceAt(KtTokens.LPAR)

            val atWhenStart = mark()
            myKotlinParsing.parseAnnotationsList(EQ_RPAR_SET)
            if (at(KtTokens.VAL_KEYWORD) || at(KtTokens.VAR_KEYWORD)) {
                val declType = myKotlinParsing.parseProperty(KotlinParsing.DeclarationParsingMode.LOCAL)

                atWhenStart.done(declType)
                atWhenStart.setCustomEdgeTokenBinders(PrecedingDocCommentsBinder, TrailingCommentsBinder)
            } else {
                atWhenStart.drop()
                parseExpression()
            }

            expect(KtTokens.RPAR, "Expecting ')'")
        }
        myBuilder.restoreNewlinesState()

        // Parse when block
        myBuilder.enableNewlines()
        if (expect(KtTokens.LBRACE, "Expecting '{'")) {
            while (!eof() && !at(KtTokens.RBRACE)) {
                parseWhenEntry()
            }

            expect(KtTokens.RBRACE, "Expecting '}'")
        }
        myBuilder.restoreNewlinesState()

        `when`.done(KtNodeTypes.WHEN)
    }

    /*
     * whenEntry
     *   // TODO : consider empty after ->
     *   : whenCondition{","} whenEntryGuard? "->" element SEMI
     *   : "else" whenEntryGuard? "->" element SEMI
     *   ;
     */
    private fun parseWhenEntry() {
        val entry = mark()

        if (at(KtTokens.ELSE_KEYWORD)) {
            advance() // ELSE_KEYWORD

            parseWhenEntryGuardOrSuggest()

            if (!at(KtTokens.ARROW)) {
                errorUntil("Expecting '->'", TokenSet.create(KtTokens.ARROW, KtTokens.LBRACE, KtTokens.RBRACE, KtTokens.EOL_OR_SEMICOLON))
            }

            if (at(KtTokens.ARROW)) {
                advance() // ARROW

                if (atSet(WHEN_CONDITION_RECOVERY_SET)) {
                    error("Expecting an element")
                } else {
                    parseControlStructureBody()
                }
            } else if (at(KtTokens.LBRACE)) { // no arrow, probably it's simply missing
                parseControlStructureBody()
            } else if (!atSet(WHEN_CONDITION_RECOVERY_SET)) {
                errorAndAdvance("Expecting '->'")
            }
        } else {
            parseWhenEntryNotElse()
        }

        entry.done(KtNodeTypes.WHEN_ENTRY)
        consumeIf(KtTokens.SEMICOLON)
    }

    /*
     * : whenCondition{","} whenEntryGuard? "->" element SEMI
     */
    private fun parseWhenEntryNotElse() {
        while (true) {
            while (at(KtTokens.COMMA)) errorAndAdvance("Expecting a when-condition")
            parseWhenCondition()
            if (!at(KtTokens.COMMA)) break
            advance() // COMMA
            if (at(KtTokens.ARROW)) {
                break
            }
        }

        parseWhenEntryGuardOrSuggest()

        expect(KtTokens.ARROW, "Expecting '->'", WHEN_CONDITION_RECOVERY_SET)
        if (atSet(WHEN_CONDITION_RECOVERY_SET)) {
            error("Expecting an element")
        } else {
            parseControlStructureBody()
        }
        // SEMI is consumed in parseWhenEntry
    }

    /*
     * whenCondition
     *   : expression
     *   : ("in" | "!in") expression
     *   : ("is" | "!is") isRHS
     *   ;
     */
    private fun parseWhenCondition() {
        val condition = mark()
        myBuilder.disableNewlines()
        when (tokenId) {
            KtTokens.IN_KEYWORD_Id, KtTokens.NOT_IN_Id -> {
                val mark = mark()
                advance() // IN_KEYWORD or NOT_IN
                mark.done(KtNodeTypes.OPERATION_REFERENCE)


                if (atSet(WHEN_CONDITION_RECOVERY_SET_WITH_ARROW)) {
                    error("Expecting an element")
                } else {
                    parseExpression()
                }
                condition.done(KtNodeTypes.WHEN_CONDITION_IN_RANGE)
            }
            KtTokens.IS_KEYWORD_Id, KtTokens.NOT_IS_Id -> {
                advance() // IS_KEYWORD or NOT_IS

                if (atSet(WHEN_CONDITION_RECOVERY_SET_WITH_ARROW)) {
                    error("Expecting a type")
                } else {
                    myKotlinParsing.parseTypeRef()
                }
                condition.done(KtNodeTypes.WHEN_CONDITION_IS_PATTERN)
            }
            KtTokens.RBRACE_Id, KtTokens.ELSE_KEYWORD_Id, KtTokens.ARROW_Id, KtTokens.DOT_Id -> {
                error("Expecting an expression, is-condition or in-condition")
                condition.done(KtNodeTypes.WHEN_CONDITION_EXPRESSION)
            }
            else -> {
                parseExpression()
                condition.done(KtNodeTypes.WHEN_CONDITION_EXPRESSION)
            }
        }
        myBuilder.restoreNewlinesState()
    }

    private fun parseWhenEntryGuardOrSuggest() {
        if (at(KtTokens.ANDAND)) {
            errorUntil(
                "Unexpected '&&', use 'if' to introduce additional conditions; see https://kotl.in/guards-in-when", TokenSet.create(
                    KtTokens.LBRACE, KtTokens.RBRACE, KtTokens.ARROW
                )
            )
        } else if (at(KtTokens.IF_KEYWORD)) {
            parseWhenEntryGuard()
        }
    }

    /*
     * whenEntryGuard
     *   : "if" expression
     *   ;
     */
    private fun parseWhenEntryGuard() {
        assert(_at(KtTokens.IF_KEYWORD))

        val guard = mark()
        advance() // IF_KEYWORD
        parseExpression()
        guard.done(KtNodeTypes.WHEN_ENTRY_GUARD)
    }

    /*
     * arrayAccess
     *   : "[" element{","} "]"
     *   ;
     */
    private fun parseArrayAccess() {
        parseAsCollectionLiteralExpression(KtNodeTypes.INDICES, false, "Expecting an index element")
    }

    /*
     * collectionLiteral
     *   : "[" element{","}? "]"
     *   ;
     */
    private fun parseCollectionLiteralExpression() {
        parseAsCollectionLiteralExpression(KtNodeTypes.COLLECTION_LITERAL_EXPRESSION, true, "Expecting an element")
    }

    private fun parseAsCollectionLiteralExpression(nodeType: IElementType, canBeEmpty: Boolean, missingElementErrorMessage: String) {
        assert(_at(KtTokens.LBRACKET))

        val innerExpressions = mark()

        myBuilder.disableNewlines()
        advance() // LBRACKET

        if (!canBeEmpty && at(KtTokens.RBRACKET)) {
            error(missingElementErrorMessage)
        } else {
            parseInnerExpressions(missingElementErrorMessage)
        }

        expect(KtTokens.RBRACKET, "Expecting ']'")
        myBuilder.restoreNewlinesState()

        innerExpressions.done(nodeType)
    }

    private fun parseInnerExpressions(missingElementErrorMessage: String) {
        while (true) {
            if (at(KtTokens.COMMA)) errorAndAdvance(missingElementErrorMessage)
            if (at(KtTokens.RBRACKET)) {
                break
            }
            parseExpression()

            if (!at(KtTokens.COMMA)) break
            advance() // COMMA
        }
    }

    fun parseContractDescriptionBlock() {
        assert(_at(KtTokens.CONTRACT_KEYWORD))

        advance() // CONTRACT_KEYWORD

        parseContractEffectList()
    }

    private fun parseContractEffectList() {
        val block = mark()

        expect(KtTokens.LBRACKET, "Expecting '['")
        myBuilder.enableNewlines()

        parseContractEffects()

        expect(KtTokens.RBRACKET, "Expecting ']'")
        myBuilder.restoreNewlinesState()

        block.done(KtNodeTypes.CONTRACT_EFFECT_LIST)
    }

    private fun parseContractEffects() {
        while (true) {
            if (at(KtTokens.COMMA)) errorAndAdvance("Expecting a contract effect")
            if (at(KtTokens.RBRACKET)) {
                break
            }
            val effect = mark()
            parseExpression()
            effect.done(KtNodeTypes.CONTRACT_EFFECT)

            if (!at(KtTokens.COMMA)) break
            advance() // COMMA
        }
    }

    /*
     * SimpleName
     */
    fun parseSimpleNameExpression() {
        val simpleName = mark()
        expect(KtTokens.IDENTIFIER, "Expecting an identifier")
        simpleName.done(KtNodeTypes.REFERENCE_EXPRESSION)
    }

    /*
     * modifiers declarationRest
     */
    private fun parseLocalDeclaration(rollbackIfDefinitelyNotExpression: Boolean, isScriptTopLevel: Boolean): Boolean {
        val decl = mark()
        val detector = KotlinParsing.ModifierDetector()
        myKotlinParsing.parseModifierList(detector, TokenSet.EMPTY)

        val declType = parseLocalDeclarationRest(detector, rollbackIfDefinitelyNotExpression, isScriptTopLevel)

        if (declType != null) {
            // we do not attach preceding comments (non-doc) to local variables because they are likely commenting a few statements below
            closeDeclarationWithCommentBinders(
                decl, declType,
                declType !== KtNodeTypes.PROPERTY && declType !== KtNodeTypes.DESTRUCTURING_DECLARATION
            )
            return true
        } else {
            decl.rollbackTo()
            return false
        }
    }

    /*
     * functionLiteral  // one can use "it" as a parameter name
     *   : "{" expressions "}"
     *   : "{" (modifiers SimpleName (":" type)?){","} "->" statements "}"
     *   ;
     */
    private fun parseFunctionLiteral() {
        parseFunctionLiteral(preferBlock = false, collapse = true)
    }

    /**
     * If it has no ->, it's a block, otherwise a function literal
     *
     * Please update [org.jetbrains.kotlin.BlockExpressionElementType.isParsable] if any changes occurs!
     */
    fun parseFunctionLiteral(preferBlock: Boolean, collapse: Boolean) {
        assert(_at(KtTokens.LBRACE))

        val literalExpression = mark()

        val literal = mark()

        myBuilder.enableNewlines()
        advance() // LBRACE

        var paramsFound = false

        val token = tt()
        if (token === KtTokens.ARROW) {
            //   { -> ...}
            mark().done(KtNodeTypes.VALUE_PARAMETER_LIST)
            advance() // ARROW
            paramsFound = true
        } else if (token === KtTokens.IDENTIFIER || token === KtTokens.COLON || token === KtTokens.LPAR) {
            // Try to parse a simple name list followed by an ARROW
            //   {a -> ...}
            //   {a, b -> ...}
            //   {(a, b) -> ... }
            val rollbackMarker = mark()
            val nextToken = lookahead(1)
            val preferParamsToExpressions = (nextToken === KtTokens.COMMA || nextToken === KtTokens.COLON)
            parseFunctionLiteralParameterList()

            paramsFound = if (preferParamsToExpressions) rollbackOrDrop(
                rollbackMarker,
                KtTokens.ARROW,
                "An -> is expected",
                KtTokens.RBRACE
            ) else rollbackOrDropAt(rollbackMarker, KtTokens.ARROW)
        }

        if (!paramsFound && preferBlock) {
            literal.drop()
            parseStatements()
            expect(KtTokens.RBRACE, "Expecting '}'")
            literalExpression.done(KtNodeTypes.BLOCK)
            myBuilder.restoreNewlinesState()

            return
        }

        if (collapse && isLazy) {
            myKotlinParsing.advanceBalancedBlock()
            literal.done(KtNodeTypes.FUNCTION_LITERAL)
            literalExpression.collapse(KtNodeTypes.LAMBDA_EXPRESSION)
        } else {
            val body = mark()
            parseStatements()

            body.done(KtNodeTypes.BLOCK)
            body.setCustomEdgeTokenBinders(PRECEDING_ALL_COMMENTS_BINDER, TRAILING_ALL_COMMENTS_BINDER)

            expect(KtTokens.RBRACE, "Expecting '}'")
            literal.done(KtNodeTypes.FUNCTION_LITERAL)
            literalExpression.done(KtNodeTypes.LAMBDA_EXPRESSION)
        }

        myBuilder.restoreNewlinesState()
    }

    private fun rollbackOrDropAt(rollbackMarker: PsiBuilder.Marker, dropAt: IElementType?): Boolean {
        if (at(dropAt)) {
            advance() // dropAt
            rollbackMarker.drop()
            return true
        }
        rollbackMarker.rollbackTo()
        return false
    }

    private fun rollbackOrDrop(
        rollbackMarker: PsiBuilder.Marker,
        expected: KtToken?, expectMessage: String?,
        validForDrop: IElementType?
    ): Boolean {
        if (at(expected)) {
            advance() // dropAt
            rollbackMarker.drop()
            return true
        } else if (at(validForDrop)) {
            rollbackMarker.drop()
            expect(expected, expectMessage!!)
            return true
        }

        rollbackMarker.rollbackTo()
        return false
    }


    /*
     * lambdaParameter{","}
     *
     * lambdaParameter
     *   : variableDeclarationEntry
     *   : multipleVariableDeclarations (":" type)?
     */
    private fun parseFunctionLiteralParameterList() {
        val parameterList = mark()

        while (!eof()) {
            if (at(KtTokens.ARROW)) {
                break
            }
            val parameter = mark()

            if (at(KtTokens.COLON)) {
                error("Expecting parameter name")
            } else if (at(KtTokens.LPAR)) {
                val destructuringDeclaration = mark()
                myKotlinParsing.parseMultiDeclarationName(
                    TOKEN_SET_TO_FOLLOW_AFTER_DESTRUCTURING_DECLARATION_IN_LAMBDA,
                    TOKEN_SET_TO_FOLLOW_AFTER_DESTRUCTURING_DECLARATION_IN_LAMBDA_RECOVERY
                )
                destructuringDeclaration.done(KtNodeTypes.DESTRUCTURING_DECLARATION)
            } else {
                expect(KtTokens.IDENTIFIER, "Expecting parameter name", ARROW_SET)
            }

            if (at(KtTokens.COLON)) {
                advance() // COLON
                myKotlinParsing.parseTypeRef(ARROW_COMMA_SET)
            }
            parameter.done(KtNodeTypes.VALUE_PARAMETER)

            if (at(KtTokens.ARROW)) {
                break
            } else if (at(KtTokens.COMMA)) {
                advance() // COMMA
            } else {
                error("Expecting '->' or ','")
                break
            }
        }

        parameterList.done(KtNodeTypes.VALUE_PARAMETER_LIST)
    }

    /*
         * expressions
         *   : SEMI* statement{SEMI+} SEMI*
         */
    /*
     * expressions
     *   : SEMI* statement{SEMI+} SEMI*
     */
    @JvmOverloads
    fun parseStatements(isScriptTopLevel: Boolean = false) {
        while (at(KtTokens.SEMICOLON)) advance() // SEMICOLON

        while (!eof() && !at(KtTokens.RBRACE)) {
            if (!atSet(STATEMENT_FIRST)) {
                errorAndAdvance("Expecting an element")
            }
            if (atSet(STATEMENT_FIRST)) {
                parseStatement(isScriptTopLevel)
            }
            if (at(KtTokens.SEMICOLON)) {
                while (at(KtTokens.SEMICOLON)) advance() // SEMICOLON
            } else if (at(KtTokens.RBRACE)) {
                break
            } else if (!isScriptTopLevel && !myBuilder.newlineBeforeCurrentToken()) {
                val severalStatementsError = "Unexpected tokens (use ';' to separate expressions on the same line)"

                if (atSet(STATEMENT_NEW_LINE_QUICK_RECOVERY_SET)) {
                    error(severalStatementsError)
                } else {
                    errorUntil(severalStatementsError, TokenSet.create(KtTokens.EOL_OR_SEMICOLON, KtTokens.LBRACE, KtTokens.RBRACE))
                }
            }
        }
    }

    /*
     * statement
     *  : declaration
     *  : blockLevelExpression
     *  ;
     */
    private fun parseStatement(isScriptTopLevel: Boolean) {
        if (!parseLocalDeclaration( /* rollbackIfDefinitelyNotExpression = */false,  /* isScriptTopLevel = */isScriptTopLevel)) {
            if (!atSet(EXPRESSION_FIRST)) {
                errorAndAdvance("Expecting a statement")
            } else if (isScriptTopLevel) {
                val scriptInitializer = mark()
                parseBlockLevelExpression()
                scriptInitializer.done(KtNodeTypes.SCRIPT_INITIALIZER)
            } else {
                parseBlockLevelExpression()
            }
        }
    }

    /*
     * blockLevelExpression
     *  : annotations + ("\n")+ expression
     *  ;
     */
    private fun parseBlockLevelExpression() {
        if (at(KtTokens.AT)) {
            val expression = mark()
            myKotlinParsing.parseAnnotations(KotlinParsing.AnnotationParsingMode.DEFAULT)

            if (!myBuilder.newlineBeforeCurrentToken()) {
                expression.rollbackTo()
                parseExpression()
                return
            }

            parseBlockLevelExpression()
            expression.done(KtNodeTypes.ANNOTATED_EXPRESSION)
            return
        }

        parseExpression()
    }

    /*
     * declaration
     *   : function
     *   : property
     *   : extension
     *   : class
     *   : typeAlias
     *   : object
     *   ;
     */
    private fun parseLocalDeclarationRest(
        modifierDetector: KotlinParsing.ModifierDetector,
        failIfDefinitelyNotExpression: Boolean,
        isScriptTopLevel: Boolean
    ): IElementType? {
        val keywordToken = tt()
        if (failIfDefinitelyNotExpression) {
            if (keywordToken !== KtTokens.FUN_KEYWORD) return null

            return myKotlinParsing.parseFunction( /* failIfIdentifierExists = */true)
        }

        if (keywordToken === KtTokens.OBJECT_KEYWORD) {
            // Object expression may appear at the statement position: should parse it
            // as expression instead of object declaration
            // sample:
            // {
            //   object : Thread() {
            //   }
            // }
            val lookahead = lookahead(1)
            if (lookahead === KtTokens.COLON || lookahead === KtTokens.LBRACE) {
                return null
            }
        }

        return myKotlinParsing.parseCommonDeclaration(
            modifierDetector, KotlinParsing.NameParsingMode.REQUIRED,
            if (isScriptTopLevel) KotlinParsing.DeclarationParsingMode.SCRIPT_TOPLEVEL else KotlinParsing.DeclarationParsingMode.LOCAL
        )
    }

    /*
     * doWhile
     *   : "do" element "while" "(" element ")"
     *   ;
     */
    private fun parseDoWhile() {
        assert(_at(KtTokens.DO_KEYWORD))

        val loop = mark()

        advance() // DO_KEYWORD

        if (!at(KtTokens.WHILE_KEYWORD)) {
            parseLoopBody()
        }

        if (expect(KtTokens.WHILE_KEYWORD, "Expecting 'while' followed by a post-condition")) {
            parseCondition()
        }

        loop.done(KtNodeTypes.DO_WHILE)
    }

    /*
     * while
     *   : "while" "(" element ")" element
     *   ;
     */
    private fun parseWhile() {
        assert(_at(KtTokens.WHILE_KEYWORD))

        val loop = mark()

        advance() // WHILE_KEYWORD

        parseCondition()

        parseLoopBody()

        loop.done(KtNodeTypes.WHILE)
    }

    /*
     * for
     *   : "for" "(" annotations ("val" | "var")? (multipleVariableDeclarations | variableDeclarationEntry) "in" expression ")" expression
     *   ;
     *
     *   TODO: empty loop body (at the end of the block)?
     */
    private fun parseFor() {
        assert(_at(KtTokens.FOR_KEYWORD))

        val loop = mark()

        advance() // FOR_KEYWORD

        if (expect(KtTokens.LPAR, "Expecting '(' to open a loop range", EXPRESSION_FIRST)) {
            myBuilder.disableNewlines()

            if (!at(KtTokens.RPAR)) {
                val parameter = mark()

                if (!at(KtTokens.IN_KEYWORD)) {
                    myKotlinParsing.parseModifierList(IN_KEYWORD_R_PAR_COLON_SET)
                }

                if (at(KtTokens.VAL_KEYWORD) || at(KtTokens.VAR_KEYWORD)) advance() // VAL_KEYWORD or VAR_KEYWORD


                if (at(KtTokens.LPAR)) {
                    val destructuringDeclaration = mark()
                    myKotlinParsing.parseMultiDeclarationName(IN_KEYWORD_L_BRACE_SET, IN_KEYWORD_L_BRACE_RECOVERY_SET)
                    destructuringDeclaration.done(KtNodeTypes.DESTRUCTURING_DECLARATION)
                } else {
                    expect(KtTokens.IDENTIFIER, "Expecting a variable name", COLON_IN_KEYWORD_SET)

                    if (at(KtTokens.COLON)) {
                        advance() // COLON
                        myKotlinParsing.parseTypeRef(IN_KEYWORD_SET)
                    }
                }
                parameter.done(KtNodeTypes.VALUE_PARAMETER)

                if (expect(KtTokens.IN_KEYWORD, "Expecting 'in'", L_PAR_L_BRACE_R_PAR_SET)) {
                    val range = mark()
                    parseExpression()
                    range.done(KtNodeTypes.LOOP_RANGE)
                }
            } else {
                error("Expecting a variable name")
            }

            expectNoAdvance(KtTokens.RPAR, "Expecting ')'")
            myBuilder.restoreNewlinesState()
        }

        parseLoopBody()

        loop.done(KtNodeTypes.FOR)
    }

    private fun parseControlStructureBody() {
        if (!parseAnnotatedLambda( /* preferBlock = */true)) {
            parseBlockLevelExpression()
        }
    }

    /*
     * element
     */
    private fun parseLoopBody() {
        val body = mark()
        if (!at(KtTokens.SEMICOLON)) {
            parseControlStructureBody()
        }
        body.done(KtNodeTypes.BODY)
    }

    /*
     * try
     *   : "try" block catchBlock* finallyBlock?
     *   ;
     * catchBlock
     *   : "catch" "(" annotations SimpleName ":" userType ")" block
     *   ;
     *
     * finallyBlock
     *   : "finally" block
     *   ;
     */
    private fun parseTry() {
        assert(_at(KtTokens.TRY_KEYWORD))

        val tryExpression = mark()

        advance() // TRY_KEYWORD

        myKotlinParsing.parseBlock()

        var catchOrFinally = false
        while (at(KtTokens.CATCH_KEYWORD)) {
            catchOrFinally = true
            val catchBlock = mark()
            advance() // CATCH_KEYWORD

            if (atSet(TRY_CATCH_RECOVERY_TOKEN_SET)) {
                error("Expecting exception variable declaration")
            } else {
                val parameters = mark()
                expect(KtTokens.LPAR, "Expecting '('", TRY_CATCH_RECOVERY_TOKEN_SET)
                if (!atSet(TRY_CATCH_RECOVERY_TOKEN_SET)) {
                    myKotlinParsing.parseValueParameter( /*typeRequired = */true)
                    if (at(KtTokens.COMMA)) {
                        advance() // trailing comma
                    }
                    expect(KtTokens.RPAR, "Expecting ')'", TRY_CATCH_RECOVERY_TOKEN_SET)
                } else {
                    error("Expecting exception variable declaration")
                }
                parameters.done(KtNodeTypes.VALUE_PARAMETER_LIST)
            }

            if (at(KtTokens.LBRACE)) {
                myKotlinParsing.parseBlock()
            } else {
                error("Expecting a block: { ... }")
            }
            catchBlock.done(KtNodeTypes.CATCH)
        }

        if (at(KtTokens.FINALLY_KEYWORD)) {
            catchOrFinally = true
            val finallyBlock = mark()

            advance() // FINALLY_KEYWORD

            myKotlinParsing.parseBlock()

            finallyBlock.done(KtNodeTypes.FINALLY)
        }

        if (!catchOrFinally) {
            error("Expecting 'catch' or 'finally'")
        }

        tryExpression.done(KtNodeTypes.TRY)
    }

    /*
     * if
     *   : "if" "(" element ")" element SEMI? ("else" element)?
     *   ;
     */
    private fun parseIf() {
        assert(_at(KtTokens.IF_KEYWORD))

        val marker = mark()

        advance() //IF_KEYWORD

        parseCondition()

        val thenBranch = mark()
        if (!at(KtTokens.ELSE_KEYWORD) && !at(KtTokens.SEMICOLON)) {
            parseControlStructureBody()
        }
        if (at(KtTokens.SEMICOLON) && lookahead(1) === KtTokens.ELSE_KEYWORD) {
            advance() // SEMICOLON
        }
        thenBranch.done(KtNodeTypes.THEN)

        // lookahead for arrow is needed to prevent capturing of whenEntry like "else -> "
        if (at(KtTokens.ELSE_KEYWORD) && lookahead(1) !== KtTokens.ARROW) {
            advance() // ELSE_KEYWORD

            val elseBranch = mark()
            if (!at(KtTokens.SEMICOLON)) {
                parseControlStructureBody()
            }
            elseBranch.done(KtNodeTypes.ELSE)
        }

        marker.done(KtNodeTypes.IF)
    }

    /*
     * "(" element ")"
     */
    private fun parseCondition() {
        myBuilder.disableNewlines()

        if (expect(KtTokens.LPAR, "Expecting a condition in parentheses '(...)'", EXPRESSION_FIRST)) {
            val condition = mark()
            parseExpression()
            condition.done(KtNodeTypes.CONDITION)
            expect(KtTokens.RPAR, "Expecting ')")
        }

        myBuilder.restoreNewlinesState()
    }

    /*
     * : "continue" getEntryPoint?
     * : "break" getEntryPoint?
     */
    private fun parseJump(type: IElementType) {
        assert(_at(KtTokens.BREAK_KEYWORD) || _at(KtTokens.CONTINUE_KEYWORD))

        val marker = mark()

        advance() // BREAK_KEYWORD or CONTINUE_KEYWORD

        parseLabelReferenceWithNoWhitespace()

        marker.done(type)
    }

    /*
     * "return" getEntryPoint? element?
     */
    private fun parseReturn() {
        assert(_at(KtTokens.RETURN_KEYWORD))

        val returnExpression = mark()

        advance() // RETURN_KEYWORD

        parseLabelReferenceWithNoWhitespace()

        if (atSet(EXPRESSION_FIRST) && !at(KtTokens.EOL_OR_SEMICOLON)) parseExpression()

        returnExpression.done(KtNodeTypes.RETURN)
    }

    /*
     * labelReference?
     */
    private fun parseLabelReferenceWithNoWhitespace() {
        if (at(KtTokens.AT) && !myBuilder.newlineBeforeCurrentToken()) {
            if (KtTokens.WHITE_SPACE_OR_COMMENT_BIT_SET.contains(myBuilder.rawLookup(-1))) {
                error("There should be no space or comments before '@' in label reference")
            }
            parseLabelReference()
        }
    }

    /*
     * IDENTIFIER "@"
     */
    fun parseLabelDefinition() {
        assert(this.isAtLabelDefinitionOrMissingIdentifier) { "Callers must check that current token is IDENTIFIER followed with '@'" }

        val labelWrap = mark()
        val mark = mark()

        if (at(KtTokens.AT)) {
            errorAndAdvance("Expecting identifier before '@' in label definition")
            labelWrap.drop()
            mark.drop()
            return
        }

        advance() // IDENTIFIER
        advance() // AT

        mark.done(KtNodeTypes.LABEL)

        labelWrap.done(KtNodeTypes.LABEL_QUALIFIER)
    }

    /*
     * "@" IDENTIFIER
     */
    private fun parseLabelReference() {
        assert(_at(KtTokens.AT))

        val labelWrap = mark()

        val mark = mark()

        if (myBuilder.rawLookup(1) !== KtTokens.IDENTIFIER) {
            errorAndAdvance("Label must be named") // AT
            labelWrap.drop()
            mark.drop()
            return
        }

        advance() // AT
        advance() // IDENTIFIER

        mark.done(KtNodeTypes.LABEL)

        labelWrap.done(KtNodeTypes.LABEL_QUALIFIER)
    }

    /*
     * : "throw" element
     */
    private fun parseThrow() {
        assert(_at(KtTokens.THROW_KEYWORD))

        val marker = mark()

        advance() // THROW_KEYWORD

        parseExpression()

        marker.done(KtNodeTypes.THROW)
    }

    /*
     * "(" expression ")"
     */
    private fun parseParenthesizedExpression() {
        assert(_at(KtTokens.LPAR))

        val mark = mark()

        myBuilder.disableNewlines()
        advance() // LPAR
        if (at(KtTokens.RPAR)) {
            error("Expecting an expression")
        } else {
            parseExpression()
        }

        expect(KtTokens.RPAR, "Expecting ')'")
        myBuilder.restoreNewlinesState()

        mark.done(KtNodeTypes.PARENTHESIZED)
    }

    /*
     * "this" label?
     */
    private fun parseThisExpression() {
        assert(_at(KtTokens.THIS_KEYWORD))
        val mark = mark()

        val thisReference = mark()
        advance() // THIS_KEYWORD
        thisReference.done(KtNodeTypes.REFERENCE_EXPRESSION)

        parseLabelReferenceWithNoWhitespace()

        mark.done(KtNodeTypes.THIS_EXPRESSION)
    }

    /*
     * "this" ("<" type ">")? label?
     */
    private fun parseSuperExpression() {
        assert(_at(KtTokens.SUPER_KEYWORD))
        val mark = mark()

        val superReference = mark()
        advance() // SUPER_KEYWORD
        superReference.done(KtNodeTypes.REFERENCE_EXPRESSION)

        if (at(KtTokens.LT)) {
            // This may be "super < foo" or "super<foo>", thus the backtracking
            val supertype = mark()

            myBuilder.disableNewlines()
            advance() // LT

            myKotlinParsing.parseTypeRef()

            if (at(KtTokens.GT)) {
                advance() // GT
                supertype.drop()
            } else {
                supertype.rollbackTo()
            }
            myBuilder.restoreNewlinesState()
        }
        parseLabelReferenceWithNoWhitespace()

        mark.done(KtNodeTypes.SUPER_EXPRESSION)
    }

    /*
     * valueArguments
     *   : "(" (SimpleName "=")? "*"? element{","} ")"
     *   ;
     */
    fun parseValueArgumentList() {
        val list = mark()

        myBuilder.disableNewlines()

        if (expect(KtTokens.LPAR, "Expecting an argument list", EXPRESSION_FOLLOW)) {
            if (!at(KtTokens.RPAR)) {
                while (true) {
                    while (at(KtTokens.COMMA)) errorAndAdvance("Expecting an argument")
                    parseValueArgument()
                    if (at(KtTokens.COLON) && lookahead(1) === KtTokens.IDENTIFIER) {
                        errorAndAdvance("Unexpected type specification", 2)
                    }
                    if (!at(KtTokens.COMMA)) {
                        if (atSet(EXPRESSION_FIRST)) {
                            error("Expecting ','")
                            continue
                        } else {
                            break
                        }
                    }
                    advance() // COMMA
                    if (at(KtTokens.RPAR)) {
                        break
                    }
                }
            }

            expect(KtTokens.RPAR, "Expecting ')'", EXPRESSION_FOLLOW)
        }

        myBuilder.restoreNewlinesState()

        list.done(KtNodeTypes.VALUE_ARGUMENT_LIST)
    }

    /*
     * (SimpleName "=")? "*"? element
     */
    private fun parseValueArgument() {
        val argument = mark()
        if (at(KtTokens.IDENTIFIER) && lookahead(1) === KtTokens.EQ) {
            val argName = mark()
            val reference = mark()
            advance() // IDENTIFIER
            reference.done(KtNodeTypes.REFERENCE_EXPRESSION)
            argName.done(KtNodeTypes.VALUE_ARGUMENT_NAME)
            advance() // EQ
        }
        if (at(KtTokens.MUL)) {
            advance() // MUL
        }
        parseExpression()
        argument.done(KtNodeTypes.VALUE_ARGUMENT)
    }

    /*
     * "object" (":" delegationSpecifier{","})? classBody // Cannot make class body optional: foo(object : F, A)
     */
    fun parseObjectLiteral() {
        val literal = mark()
        val declaration = mark()
        myKotlinParsing.parseObject(KotlinParsing.NameParsingMode.PROHIBITED, false) // Body is not optional because of foo(object : A, B)
        declaration.done(KtNodeTypes.OBJECT_DECLARATION)
        literal.done(KtNodeTypes.OBJECT_LITERAL)
    }

    private fun parseOneTokenExpression(type: IElementType) {
        val mark = mark()
        advance()
        mark.done(type)
    }

    override fun create(builder: SemanticWhitespaceAwarePsiBuilder): KotlinParsing {
        return myKotlinParsing.create(builder)
    }

    private fun interruptedWithNewLine(): Boolean {
        return !ALLOW_NEWLINE_OPERATIONS.contains(tt()) && myBuilder.newlineBeforeCurrentToken()
    }

    companion object {
        private val WHEN_CONDITION_RECOVERY_SET = TokenSet.create(
            KtTokens.RBRACE,
            KtTokens.IN_KEYWORD,
            KtTokens.NOT_IN,
            KtTokens.IS_KEYWORD,
            KtTokens.NOT_IS,
            KtTokens.ELSE_KEYWORD
        )
        private val WHEN_CONDITION_RECOVERY_SET_WITH_ARROW = TokenSet.create(
            KtTokens.RBRACE,
            KtTokens.IN_KEYWORD,
            KtTokens.NOT_IN,
            KtTokens.IS_KEYWORD,
            KtTokens.NOT_IS,
            KtTokens.ELSE_KEYWORD,
            KtTokens.ARROW,
            KtTokens.DOT
        )
        private val KEYWORD_TEXTS: ImmutableMap<String?, KtToken?> = tokenSetToMap(KtTokens.KEYWORDS)

        private val TOKEN_SET_TO_FOLLOW_AFTER_DESTRUCTURING_DECLARATION_IN_LAMBDA =
            TokenSet.create(KtTokens.ARROW, KtTokens.COMMA, KtTokens.COLON)
        private val TOKEN_SET_TO_FOLLOW_AFTER_DESTRUCTURING_DECLARATION_IN_LAMBDA_RECOVERY = TokenSet.orSet(
            TOKEN_SET_TO_FOLLOW_AFTER_DESTRUCTURING_DECLARATION_IN_LAMBDA, KotlinParsing.PARAMETER_NAME_RECOVERY_SET
        )
        private val EQ_RPAR_SET = TokenSet.create(KtTokens.EQ, KtTokens.RPAR)
        private val ARROW_SET = TokenSet.create(KtTokens.ARROW)
        private val ARROW_COMMA_SET = TokenSet.create(KtTokens.ARROW, KtTokens.COMMA)
        private val IN_KEYWORD_R_PAR_COLON_SET = TokenSet.create(KtTokens.IN_KEYWORD, KtTokens.RPAR, KtTokens.COLON)
        private val IN_KEYWORD_L_BRACE_SET = TokenSet.create(KtTokens.IN_KEYWORD, KtTokens.LBRACE)
        private val IN_KEYWORD_L_BRACE_RECOVERY_SET = TokenSet.orSet(IN_KEYWORD_L_BRACE_SET, KotlinParsing.PARAMETER_NAME_RECOVERY_SET)
        private val COLON_IN_KEYWORD_SET = TokenSet.create(KtTokens.COLON, KtTokens.IN_KEYWORD)
        private val L_PAR_L_BRACE_R_PAR_SET = TokenSet.create(KtTokens.LPAR, KtTokens.LBRACE, KtTokens.RPAR)
        private val IN_KEYWORD_SET = TokenSet.create(KtTokens.IN_KEYWORD)
        private val TRY_CATCH_RECOVERY_TOKEN_SET =
            TokenSet.create(KtTokens.LBRACE, KtTokens.RBRACE, KtTokens.FINALLY_KEYWORD, KtTokens.CATCH_KEYWORD)

        private fun tokenSetToMap(tokens: TokenSet): ImmutableMap<String?, KtToken?> {
            val builder = ImmutableMap.builder<String?, KtToken?>()
            for (token in tokens.getTypes()) {
                builder.put(token.toString(), token as KtToken)
            }
            return builder.build()
        }

        private val TYPE_ARGUMENT_LIST_STOPPERS = TokenSet.create(
            KtTokens.INTEGER_LITERAL,
            KtTokens.FLOAT_LITERAL,
            KtTokens.CHARACTER_LITERAL,
            KtTokens.INTERPOLATION_PREFIX,
            KtTokens.OPEN_QUOTE,
            KtTokens.PACKAGE_KEYWORD,
            KtTokens.AS_KEYWORD,
            KtTokens.TYPE_ALIAS_KEYWORD,
            KtTokens.INTERFACE_KEYWORD,
            KtTokens.CLASS_KEYWORD,
            KtTokens.THIS_KEYWORD,
            KtTokens.VAL_KEYWORD,
            KtTokens.VAR_KEYWORD,
            KtTokens.FUN_KEYWORD,
            KtTokens.FOR_KEYWORD,
            KtTokens.NULL_KEYWORD,
            KtTokens.TRUE_KEYWORD,
            KtTokens.FALSE_KEYWORD,
            KtTokens.IS_KEYWORD,
            KtTokens.THROW_KEYWORD,
            KtTokens.RETURN_KEYWORD,
            KtTokens.BREAK_KEYWORD,
            KtTokens.CONTINUE_KEYWORD,
            KtTokens.OBJECT_KEYWORD,
            KtTokens.IF_KEYWORD,
            KtTokens.TRY_KEYWORD,
            KtTokens.ELSE_KEYWORD,
            KtTokens.WHILE_KEYWORD,
            KtTokens.DO_KEYWORD,
            KtTokens.WHEN_KEYWORD,
            KtTokens.RBRACKET,
            KtTokens.RBRACE,
            KtTokens.RPAR,
            KtTokens.PLUSPLUS,
            KtTokens.MINUSMINUS,
            KtTokens.EXCLEXCL,  //            MUL,
            KtTokens.PLUS,
            KtTokens.MINUS,
            KtTokens.EXCL,
            KtTokens.DIV,
            KtTokens.PERC,
            KtTokens.LTEQ,  // TODO GTEQ,   foo<bar, baz>=x
            KtTokens.EQEQEQ,
            KtTokens.EXCLEQEQEQ,
            KtTokens.EQEQ,
            KtTokens.EXCLEQ,
            KtTokens.ANDAND,
            KtTokens.OROR,
            KtTokens.SAFE_ACCESS,
            KtTokens.ELVIS,
            KtTokens.SEMICOLON,
            KtTokens.RANGE,
            KtTokens.RANGE_UNTIL,
            KtTokens.EQ,
            KtTokens.MULTEQ,
            KtTokens.DIVEQ,
            KtTokens.PERCEQ,
            KtTokens.PLUSEQ,
            KtTokens.MINUSEQ,
            KtTokens.NOT_IN,
            KtTokens.NOT_IS,
            KtTokens.COLONCOLON,
            KtTokens.COLON
        )

        /*package*/
        @JvmField
        val EXPRESSION_FIRST: TokenSet = TokenSet.create( // Prefix
            KtTokens.MINUS, KtTokens.PLUS, KtTokens.MINUSMINUS, KtTokens.PLUSPLUS,
            KtTokens.EXCL, KtTokens.EXCLEXCL,  // Joining complex tokens makes it necessary to put EXCLEXCL here
            // Atomic

            KtTokens.COLONCOLON,  // callable reference

            KtTokens.LPAR,  // parenthesized
            // literal constant

            KtTokens.TRUE_KEYWORD, KtTokens.FALSE_KEYWORD,
            KtTokens.INTERPOLATION_PREFIX, KtTokens.OPEN_QUOTE,
            KtTokens.INTEGER_LITERAL, KtTokens.CHARACTER_LITERAL, KtTokens.FLOAT_LITERAL,
            KtTokens.NULL_KEYWORD,

            KtTokens.LBRACE,  // functionLiteral
            KtTokens.FUN_KEYWORD,  // expression function

            KtTokens.THIS_KEYWORD,  // this
            KtTokens.SUPER_KEYWORD,  // super

            KtTokens.IF_KEYWORD,  // if
            KtTokens.WHEN_KEYWORD,  // when
            KtTokens.TRY_KEYWORD,  // try
            KtTokens.OBJECT_KEYWORD,  // object
            // jump

            KtTokens.THROW_KEYWORD,
            KtTokens.RETURN_KEYWORD,
            KtTokens.CONTINUE_KEYWORD,
            KtTokens.BREAK_KEYWORD,  // loop

            KtTokens.FOR_KEYWORD,
            KtTokens.WHILE_KEYWORD,
            KtTokens.DO_KEYWORD,

            KtTokens.IDENTIFIER,  // SimpleName

            KtTokens.AT,  // Just for better recovery and maybe for annotations

            KtTokens.LBRACKET // Collection literal expression
        )

        val STATEMENT_FIRST: TokenSet = TokenSet.orSet(
            EXPRESSION_FIRST,
            TokenSet.create( // declaration
                KtTokens.FUN_KEYWORD,
                KtTokens.VAL_KEYWORD, KtTokens.VAR_KEYWORD,
                KtTokens.INTERFACE_KEYWORD,
                KtTokens.CLASS_KEYWORD,
                KtTokens.TYPE_ALIAS_KEYWORD
            ),
            KtTokens.MODIFIER_KEYWORDS
        )

        private val STATEMENT_NEW_LINE_QUICK_RECOVERY_SET = TokenSet.orSet(
            TokenSet.andSet(STATEMENT_FIRST, TokenSet.andNot(KtTokens.KEYWORDS, TokenSet.create(KtTokens.IN_KEYWORD))),
            TokenSet.create(KtTokens.EOL_OR_SEMICOLON)
        )

        /*package*/
        @JvmField
        val EXPRESSION_FOLLOW: TokenSet = TokenSet.create(
            KtTokens.EOL_OR_SEMICOLON, KtTokens.ARROW, KtTokens.COMMA, KtTokens.RBRACE, KtTokens.RPAR, KtTokens.RBRACKET
        )

        private val ALLOW_NEWLINE_OPERATIONS = TokenSet.create(
            KtTokens.DOT, KtTokens.SAFE_ACCESS,
            KtTokens.COLON, KtTokens.AS_KEYWORD, KtTokens.`AS_SAFE`,
            KtTokens.ELVIS,  // Can't allow `is` and `!is` because of when entry conditions: IS_KEYWORD, NOT_IS,
            KtTokens.ANDAND,
            KtTokens.OROR
        )

        val ALL_OPERATIONS: TokenSet

        init {
            val operations: MutableSet<IElementType?> = HashSet<IElementType?>()
            val values = Precedence.entries.toTypedArray()
            for (precedence in values) {
                operations.addAll(listOf<IElementType?>(*precedence.operations.getTypes()))
            }
            ALL_OPERATIONS = TokenSet.create(*operations.toTypedArray<IElementType?>())
        }

        init {
            val operations = KtTokens.OPERATIONS.getTypes()
            val opSet: MutableSet<IElementType?> = HashSet<IElementType?>(listOf<IElementType?>(*operations))
            val usedOperations: Array<IElementType> = ALL_OPERATIONS.getTypes()
            val usedSet: MutableSet<IElementType?> = HashSet<IElementType?>(listOf<IElementType?>(*usedOperations))

            if (opSet.size > usedSet.size) {
                opSet.removeAll(usedSet)
                assert(false) { opSet }
            }
            assert(usedSet.size == opSet.size) { "Either some ops are unused, or something a non-op is used" }

            usedSet.removeAll(opSet)

            assert(usedSet.isEmpty()) { usedSet.toString() }
        }


        private fun doneOrDrop(
            marker: PsiBuilder.Marker,
            type: IElementType,
            condition: Boolean
        ) {
            if (condition) {
                marker.done(type)
            } else {
                marker.drop()
            }
        }
    }
}
