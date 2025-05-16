/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.parser.utils

import fleet.com.intellij.platform.syntax.SyntaxElementType
import fleet.com.intellij.platform.syntax.SyntaxElementTypeSet
import fleet.com.intellij.platform.syntax.emptySyntaxElementTypeSet
import fleet.com.intellij.platform.syntax.parser.SyntaxTreeBuilder
import fleet.com.intellij.platform.syntax.syntaxElementTypeSetOf
import org.jetbrains.kotlin.kmp.lexer.KtTokens
import org.jetbrains.kotlin.kmp.lexer.KtTokens.AS_KEYWORD
import org.jetbrains.kotlin.kmp.lexer.KtTokens.AS_SAFE
import org.jetbrains.kotlin.kmp.lexer.KtTokens.BREAK_KEYWORD
import org.jetbrains.kotlin.kmp.lexer.KtTokens.CLASS_KEYWORD
import org.jetbrains.kotlin.kmp.lexer.KtTokens.CONTINUE_KEYWORD
import org.jetbrains.kotlin.kmp.lexer.KtTokens.DO_KEYWORD
import org.jetbrains.kotlin.kmp.lexer.KtTokens.ELSE_KEYWORD
import org.jetbrains.kotlin.kmp.lexer.KtTokens.FALSE_KEYWORD
import org.jetbrains.kotlin.kmp.lexer.KtTokens.FOR_KEYWORD
import org.jetbrains.kotlin.kmp.lexer.KtTokens.FUN_MODIFIER
import org.jetbrains.kotlin.kmp.lexer.KtTokens.IF_KEYWORD
import org.jetbrains.kotlin.kmp.lexer.KtTokens.INTERFACE_KEYWORD
import org.jetbrains.kotlin.kmp.lexer.KtTokens.IN_MODIFIER
import org.jetbrains.kotlin.kmp.lexer.KtTokens.IS_KEYWORD
import org.jetbrains.kotlin.kmp.lexer.KtTokens.NOT_IN
import org.jetbrains.kotlin.kmp.lexer.KtTokens.NOT_IS
import org.jetbrains.kotlin.kmp.lexer.KtTokens.NULL_KEYWORD
import org.jetbrains.kotlin.kmp.lexer.KtTokens.OBJECT_KEYWORD
import org.jetbrains.kotlin.kmp.lexer.KtTokens.PACKAGE_KEYWORD
import org.jetbrains.kotlin.kmp.lexer.KtTokens.RETURN_KEYWORD
import org.jetbrains.kotlin.kmp.lexer.KtTokens.SUPER_KEYWORD
import org.jetbrains.kotlin.kmp.lexer.KtTokens.THIS_KEYWORD
import org.jetbrains.kotlin.kmp.lexer.KtTokens.THROW_KEYWORD
import org.jetbrains.kotlin.kmp.lexer.KtTokens.TRUE_KEYWORD
import org.jetbrains.kotlin.kmp.lexer.KtTokens.TRY_KEYWORD
import org.jetbrains.kotlin.kmp.lexer.KtTokens.TYPE_ALIAS_KEYWORD
import org.jetbrains.kotlin.kmp.lexer.KtTokens.VAL_KEYWORD
import org.jetbrains.kotlin.kmp.lexer.KtTokens.VAR_KEYWORD
import org.jetbrains.kotlin.kmp.lexer.KtTokens.WHEN_KEYWORD
import org.jetbrains.kotlin.kmp.lexer.KtTokens.WHILE_KEYWORD
import org.jetbrains.kotlin.kmp.parser.KtNodeTypes
import kotlin.collections.set

internal open class KotlinExpressionParsing(
    builder: SemanticWhitespaceAwareSyntaxBuilder,
    private val kotlinParsing: KotlinParsing,
    isLazy: Boolean = true
) : AbstractKotlinParsing(builder, isLazy) {
    companion object {
        private val WHEN_CONDITION_RECOVERY_SET = syntaxElementTypeSetOf(
            KtTokens.RBRACE,
            IN_MODIFIER,
            NOT_IN,
            IS_KEYWORD,
            NOT_IS,
            ELSE_KEYWORD
        )
        private val WHEN_CONDITION_RECOVERY_SET_WITH_ARROW = syntaxElementTypeSetOf(
            KtTokens.RBRACE,
            IN_MODIFIER,
            NOT_IN,
            IS_KEYWORD,
            NOT_IS,
            ELSE_KEYWORD,
            KtTokens.ARROW,
            KtTokens.DOT
        )

        private val TOKEN_SET_TO_FOLLOW_AFTER_DESTRUCTURING_DECLARATION_IN_LAMBDA =
            syntaxElementTypeSetOf(KtTokens.ARROW, KtTokens.COMMA, KtTokens.COLON)
        private val TOKEN_SET_TO_FOLLOW_AFTER_DESTRUCTURING_DECLARATION_IN_LAMBDA_RECOVERY =
            TOKEN_SET_TO_FOLLOW_AFTER_DESTRUCTURING_DECLARATION_IN_LAMBDA + KotlinParsing.PARAMETER_NAME_RECOVERY_SET
        private val EQ_RPAR_SET = syntaxElementTypeSetOf(KtTokens.EQ, KtTokens.RPAR)
        private val ARROW_SET = syntaxElementTypeSetOf(KtTokens.ARROW)
        private val ARROW_COMMA_SET = syntaxElementTypeSetOf(KtTokens.ARROW, KtTokens.COMMA)
        private val IN_KEYWORD_R_PAR_COLON_SET = syntaxElementTypeSetOf(IN_MODIFIER, KtTokens.RPAR, KtTokens.COLON)
        private val IN_KEYWORD_L_BRACE_SET = syntaxElementTypeSetOf(IN_MODIFIER, KtTokens.LBRACE)
        private val IN_KEYWORD_L_BRACE_RECOVERY_SET = IN_KEYWORD_L_BRACE_SET + KotlinParsing.PARAMETER_NAME_RECOVERY_SET
        private val COLON_IN_KEYWORD_SET = syntaxElementTypeSetOf(KtTokens.COLON, IN_MODIFIER)
        private val L_PAR_L_BRACE_R_PAR_SET = syntaxElementTypeSetOf(KtTokens.LPAR, KtTokens.LBRACE, KtTokens.RPAR)
        private val IN_KEYWORD_SET = syntaxElementTypeSetOf(IN_MODIFIER)
        private val TRY_CATCH_RECOVERY_TOKEN_SET =
            syntaxElementTypeSetOf(KtTokens.LBRACE, KtTokens.RBRACE, KtTokens.FINALLY_KEYWORD, KtTokens.CATCH_KEYWORD)

        private val TYPE_ARGUMENT_LIST_STOPPERS = syntaxElementTypeSetOf(
            KtTokens.INTEGER_LITERAL,
            KtTokens.FLOAT_LITERAL,
            KtTokens.CHARACTER_LITERAL,
            KtTokens.INTERPOLATION_PREFIX,
            KtTokens.OPEN_QUOTE,
            PACKAGE_KEYWORD,
            AS_KEYWORD,
            TYPE_ALIAS_KEYWORD,
            INTERFACE_KEYWORD,
            CLASS_KEYWORD,
            THIS_KEYWORD,
            VAL_KEYWORD,
            VAR_KEYWORD,
            FUN_MODIFIER,
            FOR_KEYWORD,
            NULL_KEYWORD,
            TRUE_KEYWORD,
            FALSE_KEYWORD,
            IS_KEYWORD,
            THROW_KEYWORD,
            RETURN_KEYWORD,
            BREAK_KEYWORD,
            CONTINUE_KEYWORD,
            OBJECT_KEYWORD,
            IF_KEYWORD,
            TRY_KEYWORD,
            ELSE_KEYWORD,
            WHILE_KEYWORD,
            DO_KEYWORD,
            WHEN_KEYWORD,
            KtTokens.RBRACKET,
            KtTokens.RBRACE,
            KtTokens.RPAR,
            KtTokens.PLUSPLUS,
            KtTokens.MINUSMINUS,
            KtTokens.EXCLEXCL,  // MUL,
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
            NOT_IN,
            NOT_IS,
            KtTokens.COLONCOLON,
            KtTokens.COLON
        )

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

        val STATEMENT_FIRST: SyntaxElementTypeSet = EXPRESSION_FIRST +
                syntaxElementTypeSetOf( // declaration
                    FUN_MODIFIER,
                    VAL_KEYWORD,
                    VAR_KEYWORD,
                    INTERFACE_KEYWORD,
                    CLASS_KEYWORD,
                    TYPE_ALIAS_KEYWORD
                ) +
                KtTokens.MODIFIERS

        // It's the result of
        //
        // ```
        // (STATEMENT_FIRST && (KEYWORDS - IN_KEYWORD)) || EOL_OR_SEMICOLON
        // ```
        //
        // a Syntax library supports only union set operation
        private val STATEMENT_NEW_LINE_QUICK_RECOVERY_SET: SyntaxElementTypeSet = syntaxElementTypeSetOf(
            TYPE_ALIAS_KEYWORD,
            CLASS_KEYWORD,
            THIS_KEYWORD,
            SUPER_KEYWORD,
            VAL_KEYWORD,
            VAR_KEYWORD,
            FUN_MODIFIER,
            FOR_KEYWORD,
            NULL_KEYWORD,
            TRUE_KEYWORD,
            FALSE_KEYWORD,
            THROW_KEYWORD,
            RETURN_KEYWORD,
            BREAK_KEYWORD,
            CONTINUE_KEYWORD,
            OBJECT_KEYWORD,
            IF_KEYWORD,
            TRY_KEYWORD,
            WHILE_KEYWORD,
            DO_KEYWORD,
            WHEN_KEYWORD,
            INTERFACE_KEYWORD,
            KtTokens.EOL_OR_SEMICOLON,
        )

        val EXPRESSION_FOLLOW: SyntaxElementTypeSet = syntaxElementTypeSetOf(
            KtTokens.EOL_OR_SEMICOLON, KtTokens.ARROW, KtTokens.COMMA, KtTokens.RBRACE, KtTokens.RPAR, KtTokens.RBRACKET
        )

        private val ALLOW_NEWLINE_OPERATIONS = syntaxElementTypeSetOf(
            KtTokens.DOT, KtTokens.SAFE_ACCESS,
            KtTokens.COLON,
            AS_KEYWORD,
            AS_SAFE,
            KtTokens.ELVIS,  // Can't allow `is` and `!is` because of when entry conditions: IS_KEYWORD, NOT_IS,
            KtTokens.ANDAND,
            KtTokens.OROR
        )

        private fun doneOrDrop(
            marker: SyntaxTreeBuilder.Marker,
            type: SyntaxElementType,
            condition: Boolean
        ) {
            if (condition) {
                marker.done(type)
            } else {
                marker.drop()
            }
        }

        // typeArguments? valueArguments : typeArguments : arrayAccess
        val POSTFIX_OPERATIONS = syntaxElementTypeSetOf(KtTokens.PLUSPLUS, KtTokens.MINUSMINUS, KtTokens.EXCLEXCL, KtTokens.DOT, KtTokens.SAFE_ACCESS)
        val PREFIX_OPERATIONS = syntaxElementTypeSetOf(KtTokens.MINUS, KtTokens.PLUS, KtTokens.MINUSMINUS, KtTokens.PLUSPLUS, KtTokens.EXCL)

        val BINARY_PRECEDENCES_TO_CURRENT_AND_HIGHER_OPERATIONS: Array<Map<SyntaxElementType, BinaryOperationPrecedence>> =
            getPrecedencesToOperations(BinaryOperationPrecedence.AS)

        val BINARY_PRECEDENCES_TO_CURRENT_AND_HIGHER_OPERATIONS_UP_TO_IS: Array<Map<SyntaxElementType, BinaryOperationPrecedence>> =
            getPrecedencesToOperations(BinaryOperationPrecedence.IN_OR_IS)

        private fun getPrecedencesToOperations(startPrecedence: BinaryOperationPrecedence): Array<Map<SyntaxElementType, BinaryOperationPrecedence>> {
            return buildList<Map<SyntaxElementType, BinaryOperationPrecedence>> {
                for (entry in BinaryOperationPrecedence.entries) {
                    add(
                        if (entry < startPrecedence) {
                            emptyMap()
                        } else {
                            val currentOrHigherOperations = mutableMapOf<SyntaxElementType, BinaryOperationPrecedence>()
                            elementAtOrNull(entry.ordinal - 1)?.let { currentOrHigherOperations.putAll(it) }
                            for (operation in entry.operations) {
                                val existingHigherOperation = currentOrHigherOperations[operation]
                                require(existingHigherOperation == null) {
                                    "All precedences have unique operations. The $operation already assigned to ${existingHigherOperation}."
                                }
                                currentOrHigherOperations[operation] = entry
                            }
                            currentOrHigherOperations
                        }
                    )
                }
            }.toTypedArray()
        }
    }

    enum class BinaryOperationPrecedence(val higher: BinaryOperationPrecedence?, val operations: SyntaxElementTypeSet) {
        AS(null, syntaxElementTypeSetOf(AS_KEYWORD, AS_SAFE)),
        MULTIPLICATIVE(AS, syntaxElementTypeSetOf(KtTokens.MUL, KtTokens.DIV, KtTokens.PERC)),
        ADDITIVE(MULTIPLICATIVE, syntaxElementTypeSetOf(KtTokens.PLUS, KtTokens.MINUS)),
        RANGE(ADDITIVE, syntaxElementTypeSetOf(KtTokens.RANGE, KtTokens.RANGE_UNTIL)),
        INFIX(RANGE, syntaxElementTypeSetOf(KtTokens.IDENTIFIER) + KtTokens.SOFT_KEYWORDS_AND_MODIFIERS),
        ELVIS(INFIX, syntaxElementTypeSetOf(KtTokens.ELVIS)),
        IN_OR_IS(ELVIS, syntaxElementTypeSetOf(IN_MODIFIER, NOT_IN, IS_KEYWORD, NOT_IS)),
        COMPARISON(IN_OR_IS, syntaxElementTypeSetOf(KtTokens.LT, KtTokens.GT, KtTokens.LTEQ, KtTokens.GTEQ)),
        EQUALITY(COMPARISON, syntaxElementTypeSetOf(KtTokens.EQEQ, KtTokens.EXCLEQ, KtTokens.EQEQEQ, KtTokens.EXCLEQEQEQ)),
        CONJUNCTION(EQUALITY, syntaxElementTypeSetOf(KtTokens.ANDAND)),
        DISJUNCTION(CONJUNCTION, syntaxElementTypeSetOf(KtTokens.OROR)),
        ASSIGNMENT(DISJUNCTION, syntaxElementTypeSetOf(KtTokens.EQ, KtTokens.PLUSEQ, KtTokens.MINUSEQ, KtTokens.MULTEQ, KtTokens.DIVEQ, KtTokens.PERCEQ)),
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
        if (!atSetWithRemap(EXPRESSION_FIRST)) {
            error("Expecting an expression")
            return
        }

        parseBinaryExpression(BinaryOperationPrecedence.ASSIGNMENT)
    }

    /*
     * element (operation element)*
     *
     * see the precedence table
     *
     * Returns `true` if the last operation is `is` expression.
     * It's handled in a special way because it doesn't use recursive parsing and
     * `is` precedence is not the highest unlike `as` operation that also doesn't use recursive parsing.
     */
    private fun parseBinaryExpression(precedence: BinaryOperationPrecedence?): Boolean {
        if (precedence == null) {
            error("Shouldn't be here")
            return false
        }

        var expression = mark()

        parsePrefixExpression()

        // Match all possible operations with the current or higher precedence at once.
        // For instance, if the precedence is COMPARISON, we can match ADDITIVE, MULTIPLICATIVE but cannot parse CONJUNCTION.
        // The lowest precedence is ASSIGNMENT.
        // All Kotlin binary operations are syntactically left-associative,
        // that's why always use higher precedence on recursive `parseBinaryExpression` call.
        // If a right-associative operation existed, the same precedence instead of the highest would be passed to the recursive call.
        val originalOperationsToPrecedences = BINARY_PRECEDENCES_TO_CURRENT_AND_HIGHER_OPERATIONS[precedence.ordinal]
        var possibleOperationsToPrecedences = originalOperationsToPrecedences
        var lastResultIsIsOperation = false

        while (!interruptedWithNewLine()) {
            val operation = tt()

            val currentPrecedence = possibleOperationsToPrecedences[operation] ?: break

            if (currentPrecedence == BinaryOperationPrecedence.INFIX && operation in KtTokens.SOFT_KEYWORDS_AND_MODIFIERS) {
                // Remap soft keywords and modifiers that are treated as infix functions since there is no mutating `atSet` call
                builder.remapCurrentToken(KtTokens.IDENTIFIER)
            }

            parseOperationReference()

            val resultType = when (currentPrecedence) {
                BinaryOperationPrecedence.AS -> {
                    kotlinParsing.parseTypeRefWithoutIntersections()
                    lastResultIsIsOperation = false
                    KtNodeTypes.BINARY_WITH_TYPE
                }
                BinaryOperationPrecedence.IN_OR_IS -> {
                    if (operation === IS_KEYWORD || operation === NOT_IS) {
                        kotlinParsing.parseTypeRefWithoutIntersections()
                        // The handling of `is` and `as` operations is special, it doesn't parse recursively and greedily.
                        // To prevent parsing of more prioritized operations after `is` (for instance, INFIX, RANGE and other),
                        // use the following map on the next operations.
                        // It's also applicable to `as`, however, AS binary precedence is the highest and no special map is needed.
                        lastResultIsIsOperation = true
                        KtNodeTypes.IS_EXPRESSION
                    } else {
                        lastResultIsIsOperation = parseBinaryExpression(currentPrecedence.higher)
                        KtNodeTypes.BINARY_EXPRESSION
                    }
                }
                else -> {
                    // Parse operations with higher precedences greedily.
                    // It handles precedences correctly.
                    lastResultIsIsOperation = parseBinaryExpression(currentPrecedence.higher)
                    KtNodeTypes.BINARY_EXPRESSION
                }
            }

            expression.done(resultType)
            expression = expression.precede()

            possibleOperationsToPrecedences = if (lastResultIsIsOperation) {
                BINARY_PRECEDENCES_TO_CURRENT_AND_HIGHER_OPERATIONS_UP_TO_IS[precedence.ordinal]
            } else {
                originalOperationsToPrecedences
            }
        }

        expression.drop()

        return lastResultIsIsOperation
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
                kotlinParsing.parseAnnotations(KotlinParsing.AnnotationParsingMode.DEFAULT)
                parsePrefixExpression()
                expression.done(KtNodeTypes.ANNOTATED_EXPRESSION)
            }
        } else {
            builder.disableJoiningComplexTokens()
            if (this.isAtLabelDefinitionOrMissingIdentifier) {
                builder.restoreJoiningComplexTokensState()
                parseLabeledExpression()
            } else if (atSetWithRemap(PREFIX_OPERATIONS)) {
                val expression = mark()

                parseOperationReference()

                builder.restoreJoiningComplexTokensState()

                parsePrefixExpression()
                expression.done(KtNodeTypes.PREFIX_EXPRESSION)
            } else {
                builder.restoreJoiningComplexTokensState()
                parsePostfixExpression()
            }
        }
    }

    /*
     * doubleColonSuffix
     *   : "::" SimpleName typeArguments?
     *   ;
     */
    private fun parseDoubleColonSuffix(expression: SyntaxTreeBuilder.Marker): Boolean {
        if (!at(KtTokens.COLONCOLON)) return false

        advance() // COLONCOLON

        if (at(CLASS_KEYWORD)) {
            advance() // CLASS_KEYWORD

            expression.done(KtNodeTypes.CLASS_LITERAL_EXPRESSION)
            return true
        }

        parseSimpleNameExpression()

        if (at(KtTokens.LT)) {
            val typeArgumentList = mark()
            if (kotlinParsing.tryParseTypeArgumentList(TYPE_ARGUMENT_LIST_STOPPERS)) {
                typeArgumentList.error("Type arguments are not allowed")
            } else {
                typeArgumentList.rollbackTo()
            }
        }

        if (at(KtTokens.LPAR) && !builder.newlineBeforeCurrentToken()) {
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
            } else if (atSetWithRemap(POSTFIX_OPERATIONS)) {
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
        if (!parseCallWithClosure()) { // do nothing if parsing of call with closure is successful
            when (tokenId) {
                KtTokens.LPAR_ID -> {
                    parseValueArgumentList()
                    parseCallWithClosure()
                }
                KtTokens.LT_ID -> {
                    val typeArgumentList = mark()
                    if (kotlinParsing.tryParseTypeArgumentList(TYPE_ARGUMENT_LIST_STOPPERS)) {
                        typeArgumentList.done(KtNodeTypes.TYPE_ARGUMENT_LIST)
                        if (!builder.newlineBeforeCurrentToken() && at(KtTokens.LPAR)) parseValueArgumentList()
                        parseCallWithClosure()
                    } else {
                        typeArgumentList.rollbackTo()
                        return false
                    }
                }
                else -> {
                    return false
                }
            }
        }

        return true
    }

    /*
     * atomicExpression typeParameters? valueParameters? functionLiteral*
     */
    private fun parseSelectorCallExpression() {
        val mark = mark()
        parseAtomicExpression()
        if (!builder.newlineBeforeCurrentToken() && parseCallSuffix()) {
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

            if (!parseAnnotatedLambda(preferBlock = false)) {
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

        val wereAnnotations = kotlinParsing.parseAnnotations(KotlinParsing.AnnotationParsingMode.DEFAULT)
        val labeled = mark()

        val wasLabel = this.isAtLabelDefinitionOrMissingIdentifier
        if (wasLabel) {
            parseLabelDefinition()
        }

        if (!at(KtTokens.LBRACE)) {
            annotated.rollbackTo()
            return false
        }

        parseFunctionLiteral(preferBlock, collapse = true)

        doneOrDrop(labeled, KtNodeTypes.LABELED_EXPRESSION, wasLabel)
        doneOrDrop(annotated, KtNodeTypes.ANNOTATED_EXPRESSION, wereAnnotations)

        return true
    }

    val isAtLabelDefinitionOrMissingIdentifier: Boolean
        get() = (atWithRemap(KtTokens.IDENTIFIER) && builder.rawLookup(1) === KtTokens.AT) || at(KtTokens.AT)

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
            KtTokens.LPAR_ID -> parseParenthesizedExpression()
            KtTokens.LBRACKET_ID -> parseCollectionLiteralExpression()
            KtTokens.THIS_KEYWORD_ID -> parseThisExpression()
            KtTokens.SUPER_KEYWORD_ID -> parseSuperExpression()
            KtTokens.OBJECT_KEYWORD_ID -> parseObjectLiteral()
            KtTokens.THROW_KEYWORD_ID -> parseThrow()
            KtTokens.RETURN_KEYWORD_ID -> parseReturn()
            KtTokens.CONTINUE_KEYWORD_ID -> parseJump(KtNodeTypes.CONTINUE)
            KtTokens.BREAK_KEYWORD_ID -> parseJump(KtNodeTypes.BREAK)
            KtTokens.IF_KEYWORD_ID -> parseIf()
            KtTokens.WHEN_KEYWORD_ID -> parseWhen()
            KtTokens.TRY_KEYWORD_ID -> parseTry()
            KtTokens.FOR_KEYWORD_ID -> parseFor()
            KtTokens.WHILE_KEYWORD_ID -> parseWhile()
            KtTokens.DO_KEYWORD_ID -> parseDoWhile()
            KtTokens.IDENTIFIER_ID -> {
                // Try to parse an anonymous function with context parameters
                if (atWithRemap(KtTokens.CONTEXT_KEYWORD) && lookahead(1) === KtTokens.LPAR) {
                    if (parseLocalDeclaration(rollbackIfDefinitelyNotExpression = true, isScriptTopLevel = false)) {
                        return true
                    } else {
                        atWithRemap(KtTokens.IDENTIFIER)
                    }
                }

                parseSimpleNameExpression()
            }
            KtTokens.LBRACE_ID -> parseFunctionLiteral()
            KtTokens.INTERPOLATION_PREFIX_ID,
            KtTokens.OPEN_QUOTE_ID -> parseStringTemplate()
            KtTokens.TRUE_KEYWORD_ID,
            KtTokens.FALSE_KEYWORD_ID -> parseOneTokenExpression(KtNodeTypes.BOOLEAN_CONSTANT)
            KtTokens.INTEGER_LITERAL_ID -> parseOneTokenExpression(KtNodeTypes.INTEGER_CONSTANT)
            KtTokens.CHARACTER_LITERAL_ID -> parseOneTokenExpression(KtNodeTypes.CHARACTER_CONSTANT)
            KtTokens.FLOAT_LITERAL_ID -> parseOneTokenExpression(KtNodeTypes.FLOAT_CONSTANT)
            KtTokens.NULL_KEYWORD_ID -> parseOneTokenExpression(KtNodeTypes.NULL)
            KtTokens.CLASS_KEYWORD_ID,
            KtTokens.INTERFACE_KEYWORD_ID,
            KtTokens.FUN_MODIFIER_ID,
            KtTokens.VAL_KEYWORD_ID,
            KtTokens.VAR_KEYWORD_ID,
            KtTokens.TYPE_ALIAS_KEYWORD_ID -> {
                if (!parseLocalDeclaration(
                        rollbackIfDefinitelyNotExpression = builder.newlineBeforeCurrentToken(),
                        isScriptTopLevel = false
                    )
                ) {
                    ok = false
                }
            }
            else -> ok = false
        }

        if (!ok) {
            // TODO: better recovery if FIRST(element) did not match
            errorWithRecovery("Expecting an element", EXPRESSION_FOLLOW + syntaxElementTypeSetOf(KtTokens.LONG_TEMPLATE_ENTRY_END))
        }

        return ok
    }

    /*
     * stringTemplate
     *   : INTERPOLATION_PREFIX OPEN_QUOTE stringTemplateElement* CLOSING_QUOTE
     *   ;
     */
    private fun parseStringTemplate() {
        require(at(KtTokens.INTERPOLATION_PREFIX) || at(KtTokens.OPEN_QUOTE))

        val template = mark()

        if (at(KtTokens.INTERPOLATION_PREFIX)) {
            val mark = mark()
            advance() // INTERPOLATION_PREFIX
            mark.done(KtNodeTypes.STRING_INTERPOLATION_PREFIX);
        }

        require(at(KtTokens.OPEN_QUOTE))
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
        when (tokenId) {
            KtTokens.REGULAR_STRING_PART_ID -> {
                val mark = mark()
                advance() // REGULAR_STRING_PART
                mark.done(KtNodeTypes.LITERAL_STRING_TEMPLATE_ENTRY)
            }
            KtTokens.ESCAPE_SEQUENCE_ID -> {
                val mark = mark()
                advance() // ESCAPE_SEQUENCE
                mark.done(KtNodeTypes.ESCAPE_STRING_TEMPLATE_ENTRY)
            }
            KtTokens.SHORT_TEMPLATE_ENTRY_START_ID -> {
                val entry = mark()
                advance() // SHORT_TEMPLATE_ENTRY_START

                if (at(THIS_KEYWORD)) {
                    val thisExpression = mark()
                    val reference = mark()
                    advance() // THIS_KEYWORD
                    reference.done(KtNodeTypes.REFERENCE_EXPRESSION)
                    thisExpression.done(KtNodeTypes.THIS_EXPRESSION)
                } else {
                    val keyword = KtTokens.getHardKeywordOrModifier(builder.tokenText)
                    if (keyword != null) {
                        builder.remapCurrentToken(keyword)
                        errorAndAdvance("Keyword cannot be used as a reference")
                    } else {
                        val reference = mark()
                        expectIdentifierWithRemap("Expecting a name")
                        reference.done(KtNodeTypes.REFERENCE_EXPRESSION)
                    }
                }

                entry.done(KtNodeTypes.SHORT_STRING_TEMPLATE_ENTRY)
            }
            KtTokens.LONG_TEMPLATE_ENTRY_START_ID -> {
                val longTemplateEntry = mark()

                advance() // LONG_TEMPLATE_ENTRY_START

                while (!eof()) {
                    val offset = builder.currentOffset

                    parseExpression()

                    if (at(KtTokens.LONG_TEMPLATE_ENTRY_END)) {
                        advance()
                        break
                    } else {
                        error("Expecting '}'")
                        if (offset == builder.currentOffset) {
                            // Prevent hang if can't advance with parseExpression()
                            advance()
                        }
                    }
                }

                longTemplateEntry.done(KtNodeTypes.LONG_STRING_TEMPLATE_ENTRY)
            }
            else -> {
                errorAndAdvance("Unexpected token in a string template")
            }
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
        require(at(WHEN_KEYWORD))

        val whenMark = mark()

        advance() // WHEN_KEYWORD

        // Parse condition
        builder.disableNewlines()
        if (at(KtTokens.LPAR)) {
            builder.advanceLexer() // Skip LPAR

            val atWhenStart = mark()
            kotlinParsing.parseAnnotationsList(EQ_RPAR_SET)
            if (at(VAL_KEYWORD) || at(VAR_KEYWORD)) {
                val declType = kotlinParsing.parseProperty(KotlinParsing.DeclarationParsingMode.LOCAL)

                atWhenStart.done(declType)
                atWhenStart.setCustomEdgeTokenBinders(PrecedingDocCommentsBinder, TrailingCommentsBinder)
            } else {
                atWhenStart.drop()
                parseExpression()
            }

            expect(KtTokens.RPAR, "Expecting ')'")
        }
        builder.restoreNewlinesState()

        // Parse when block
        builder.enableNewlines()
        if (expect(KtTokens.LBRACE, "Expecting '{'")) {
            while (!eof() && !at(KtTokens.RBRACE)) {
                parseWhenEntry()
            }

            expect(KtTokens.RBRACE, "Expecting '}'")
        }
        builder.restoreNewlinesState()

        whenMark.done(KtNodeTypes.WHEN)
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

        if (at(ELSE_KEYWORD)) {
            advance() // ELSE_KEYWORD

            parseWhenEntryGuardOrSuggest()

            if (!at(KtTokens.ARROW)) {
                errorUntil(
                    "Expecting '->'",
                    syntaxElementTypeSetOf(KtTokens.ARROW, KtTokens.LBRACE, KtTokens.RBRACE, KtTokens.EOL_OR_SEMICOLON)
                )
            }

            if (at(KtTokens.ARROW)) {
                advance() // ARROW

                if (atSetWithRemap(WHEN_CONDITION_RECOVERY_SET)) {
                    error("Expecting an element")
                } else {
                    parseControlStructureBody()
                }
            } else if (at(KtTokens.LBRACE)) { // no arrow, probably it's simply missing
                parseControlStructureBody()
            } else if (!atSetWithRemap(WHEN_CONDITION_RECOVERY_SET)) {
                errorAndAdvance("Expecting '->'")
            }
        } else {
            parseWhenEntryNotElse()
        }

        entry.done(KtNodeTypes.WHEN_ENTRY)
        consumeIfSemicolon()
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
        if (atSetWithRemap(WHEN_CONDITION_RECOVERY_SET)) {
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
        builder.disableNewlines()
        when (tokenId) {
            KtTokens.IN_MODIFIER_ID,
            KtTokens.NOT_IN_ID -> {
                val mark = mark()
                advance() // IN_KEYWORD or NOT_IN
                mark.done(KtNodeTypes.OPERATION_REFERENCE)

                if (atSetWithRemap(WHEN_CONDITION_RECOVERY_SET_WITH_ARROW)) {
                    error("Expecting an element")
                } else {
                    parseExpression()
                }
                condition.done(KtNodeTypes.WHEN_CONDITION_IN_RANGE)
            }
            KtTokens.IS_KEYWORD_ID,
            KtTokens.NOT_IS_ID -> {
                advance() // IS_KEYWORD or NOT_IS

                if (atSetWithRemap(WHEN_CONDITION_RECOVERY_SET_WITH_ARROW)) {
                    error("Expecting a type")
                } else {
                    kotlinParsing.parseTypeRef()
                }
                condition.done(KtNodeTypes.WHEN_CONDITION_IS_PATTERN)
            }
            KtTokens.RBRACE_ID,
            KtTokens.ELSE_KEYWORD_ID,
            KtTokens.ARROW_ID,
            KtTokens.DOT_ID -> {
                error("Expecting an expression, is-condition or in-condition")
                condition.done(KtNodeTypes.WHEN_CONDITION_EXPRESSION)
            }
            else -> {
                parseExpression()
                condition.done(KtNodeTypes.WHEN_CONDITION_EXPRESSION)
            }
        }
        builder.restoreNewlinesState()
    }

    private fun parseWhenEntryGuardOrSuggest() {
        when (tokenId) {
            KtTokens.ANDAND_ID -> {
                errorUntil(
                    "Unexpected '&&', use 'if' to introduce additional conditions; see https://kotl.in/guards-in-when", syntaxElementTypeSetOf(
                        KtTokens.LBRACE, KtTokens.RBRACE, KtTokens.ARROW
                    )
                )
            }
            KtTokens.IF_KEYWORD_ID -> {
                parseWhenEntryGuard()
            }
        }
    }

    /*
     * whenEntryGuard
     *   : "if" expression
     *   ;
     */
    private fun parseWhenEntryGuard() {
        require(at(IF_KEYWORD))

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

    private fun parseAsCollectionLiteralExpression(nodeType: SyntaxElementType, canBeEmpty: Boolean, missingElementErrorMessage: String) {
        require(at(KtTokens.LBRACKET))

        val innerExpressions = mark()

        builder.disableNewlines()
        advance() // LBRACKET

        if (!canBeEmpty && at(KtTokens.RBRACKET)) {
            error(missingElementErrorMessage)
        } else {
            parseInnerExpressions(missingElementErrorMessage)
        }

        expect(KtTokens.RBRACKET, "Expecting ']'")
        builder.restoreNewlinesState()

        innerExpressions.done(nodeType)
    }

    private fun parseInnerExpressions(missingElementErrorMessage: String) {
        while (true) {
            if (at(KtTokens.COMMA)) {
                errorAndAdvance(missingElementErrorMessage)
            }
            if (at(KtTokens.RBRACKET)) {
                break
            }
            parseExpression()

            if (!at(KtTokens.COMMA)) {
                break
            }
            advance() // COMMA
        }
    }

    fun parseContractDescriptionBlock() {
        require(atWithRemap(KtTokens.CONTRACT_MODIFIER))

        advance() // CONTRACT_KEYWORD

        parseContractEffectList()
    }

    private fun parseContractEffectList() {
        val block = mark()

        expect(KtTokens.LBRACKET, "Expecting '['")
        builder.enableNewlines()

        parseContractEffects()

        expect(KtTokens.RBRACKET, "Expecting ']'")
        builder.restoreNewlinesState()

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
        expectIdentifierWithRemap("Expecting an identifier")
        simpleName.done(KtNodeTypes.REFERENCE_EXPRESSION)
    }

    /*
     * modifiers declarationRest
     */
    private fun parseLocalDeclaration(rollbackIfDefinitelyNotExpression: Boolean, isScriptTopLevel: Boolean): Boolean {
        val decl = mark()
        val detector = KotlinParsing.ModifierDetector()
        kotlinParsing.parseModifierList(detector, emptySyntaxElementTypeSet())

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
        require(at(KtTokens.LBRACE))

        val literalExpression = mark()

        val literal = mark()

        builder.enableNewlines()
        advance() // LBRACE

        var paramsFound = false

        when (tokenId) {
            KtTokens.ARROW_ID -> {
                //   { -> ...}
                mark().done(KtNodeTypes.VALUE_PARAMETER_LIST)
                advance() // ARROW
                paramsFound = true
            }
            KtTokens.IDENTIFIER_ID, KtTokens.COLON_ID, KtTokens.LPAR_ID -> {
                // Try to parse a simple name list followed by an ARROW
                //   {a -> ...}
                //   {a, b -> ...}
                //   {(a, b) -> ... }
                val rollbackMarker = mark()
                val nextToken = lookahead(1)
                val preferParamsToExpressions = (nextToken === KtTokens.COMMA || nextToken === KtTokens.COLON)
                parseFunctionLiteralParameterList()

                paramsFound = rollbackOrDropArrow(rollbackMarker, preferParamsToExpressions)
            }
        }

        if (!paramsFound && preferBlock) {
            literal.drop()
            parseStatements()
            expect(KtTokens.RBRACE, "Expecting '}'")
            literalExpression.done(KtNodeTypes.BLOCK)
            builder.restoreNewlinesState()

            return
        }

        if (collapse && isLazy) {
            kotlinParsing.advanceBalancedBlock()
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

        builder.restoreNewlinesState()
    }

    private fun rollbackOrDropArrow(rollbackMarker: SyntaxTreeBuilder.Marker, preferParamsToExpressions: Boolean): Boolean {
        if (at(KtTokens.ARROW)) {
            advance() // dropAt
            rollbackMarker.drop()
            return true
        } else if (preferParamsToExpressions && at(KtTokens.RBRACE)) {
            rollbackMarker.drop()
            expect(KtTokens.ARROW, "An -> is expected")
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

            when (tokenId) {
                KtTokens.COLON_ID -> {
                    error("Expecting parameter name")
                }
                KtTokens.LPAR_ID -> {
                    val destructuringDeclaration = mark()
                    kotlinParsing.parseMultiDeclarationName(
                        TOKEN_SET_TO_FOLLOW_AFTER_DESTRUCTURING_DECLARATION_IN_LAMBDA,
                        TOKEN_SET_TO_FOLLOW_AFTER_DESTRUCTURING_DECLARATION_IN_LAMBDA_RECOVERY
                    )
                    destructuringDeclaration.done(KtNodeTypes.DESTRUCTURING_DECLARATION)
                }
                else -> {
                    expectIdentifierWithRemap("Expecting parameter name", ARROW_SET)
                }
            }

            if (at(KtTokens.COLON)) {
                advance() // COLON
                kotlinParsing.parseTypeRef(ARROW_COMMA_SET)
            }
            parameter.done(KtNodeTypes.VALUE_PARAMETER)

            when (tokenId) {
                KtTokens.ARROW_ID -> {
                    break
                }
                KtTokens.COMMA_ID -> {
                    advance() // COMMA
                }
                else -> {
                    error("Expecting '->' or ','")
                    break
                }
            }
        }

        parameterList.done(KtNodeTypes.VALUE_PARAMETER_LIST)
    }

    /*
     * expressions
     *   : SEMI* statement{SEMI+} SEMI*
     */
    fun parseStatements(isScriptTopLevel: Boolean = false) {
        while (at(KtTokens.SEMICOLON)) {
            advance()
        }

        while (!eof() && !at(KtTokens.RBRACE)) {
            if (!atSetWithRemap(STATEMENT_FIRST)) {
                errorAndAdvance("Expecting an element")
            }

            if (atSetWithRemap(STATEMENT_FIRST)) {
                parseStatement(isScriptTopLevel)
            }

            when (tokenId) {
                KtTokens.SEMICOLON_ID -> {
                    while (at(KtTokens.SEMICOLON)) {
                        advance()
                    }
                }
                KtTokens.RBRACE_ID -> {
                    break
                }
                else -> {
                    if (!isScriptTopLevel && !builder.newlineBeforeCurrentToken()) {
                        val severalStatementsError = "Unexpected tokens (use ';' to separate expressions on the same line)"

                        if (atSetWithRemap(STATEMENT_NEW_LINE_QUICK_RECOVERY_SET)) {
                            error(severalStatementsError)
                        } else {
                            errorUntil(
                                severalStatementsError,
                                syntaxElementTypeSetOf(KtTokens.EOL_OR_SEMICOLON, KtTokens.LBRACE, KtTokens.RBRACE)
                            )
                        }
                    }
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
        if (!parseLocalDeclaration(rollbackIfDefinitelyNotExpression = false, isScriptTopLevel = isScriptTopLevel)) {
            if (!atSetWithRemap(EXPRESSION_FIRST)) {
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
            kotlinParsing.parseAnnotations(KotlinParsing.AnnotationParsingMode.DEFAULT)

            if (!builder.newlineBeforeCurrentToken()) {
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
    ): SyntaxElementType? {
        val keywordToken = tt()
        if (failIfDefinitelyNotExpression) {
            if (keywordToken !== FUN_MODIFIER) return null

            return kotlinParsing.parseFunction(failIfIdentifierExists = true)
        }

        if (keywordToken === OBJECT_KEYWORD) {
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

        return kotlinParsing.parseCommonDeclaration(
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
        require(at(DO_KEYWORD))

        val loop = mark()

        advance() // DO_KEYWORD

        if (!at(WHILE_KEYWORD)) {
            parseLoopBody()
        }

        if (expect(WHILE_KEYWORD, "Expecting 'while' followed by a post-condition")) {
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
        require(at(WHILE_KEYWORD))

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
        require(at(FOR_KEYWORD))

        val loop = mark()

        advance() // FOR_KEYWORD

        if (expect(KtTokens.LPAR, "Expecting '(' to open a loop range", EXPRESSION_FIRST)) {
            builder.disableNewlines()

            if (!at(KtTokens.RPAR)) {
                val parameter = mark()

                if (!at(IN_MODIFIER)) {
                    kotlinParsing.parseModifierList(IN_KEYWORD_R_PAR_COLON_SET)
                }

                if (at(VAL_KEYWORD) || at(VAR_KEYWORD)) {
                    advance()
                }

                if (at(KtTokens.LPAR)) {
                    val destructuringDeclaration = mark()
                    kotlinParsing.parseMultiDeclarationName(IN_KEYWORD_L_BRACE_SET, IN_KEYWORD_L_BRACE_RECOVERY_SET)
                    destructuringDeclaration.done(KtNodeTypes.DESTRUCTURING_DECLARATION)
                } else {
                    expectIdentifierWithRemap("Expecting a variable name", COLON_IN_KEYWORD_SET)

                    if (at(KtTokens.COLON)) {
                        advance() // COLON
                        kotlinParsing.parseTypeRef(IN_KEYWORD_SET)
                    }
                }
                parameter.done(KtNodeTypes.VALUE_PARAMETER)

                if (expect(IN_MODIFIER, "Expecting 'in'", L_PAR_L_BRACE_R_PAR_SET)) {
                    val range = mark()
                    parseExpression()
                    range.done(KtNodeTypes.LOOP_RANGE)
                }
            } else {
                error("Expecting a variable name")
            }

            if (at(KtTokens.RPAR)) {
                advance() // expectation
            } else {
                error("Expecting ')'")
            }

            builder.restoreNewlinesState()
        }

        parseLoopBody()

        loop.done(KtNodeTypes.FOR)
    }

    private fun parseControlStructureBody() {
        if (!parseAnnotatedLambda(preferBlock = true)) {
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
        require(at(TRY_KEYWORD))

        val tryExpression = mark()

        advance() // TRY_KEYWORD

        kotlinParsing.parseBlock()

        var catchOrFinally = false
        while (atWithRemap(KtTokens.CATCH_KEYWORD)) {
            catchOrFinally = true
            val catchBlock = mark()
            advance() // CATCH_KEYWORD

            if (atSetWithRemap(TRY_CATCH_RECOVERY_TOKEN_SET)) {
                error("Expecting exception variable declaration")
            } else {
                val parameters = mark()
                expect(KtTokens.LPAR, "Expecting '('", TRY_CATCH_RECOVERY_TOKEN_SET)
                if (!atSetWithRemap(TRY_CATCH_RECOVERY_TOKEN_SET)) {
                    kotlinParsing.parseValueParameter(typeRequired = true)
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
                kotlinParsing.parseBlock()
            } else {
                error("Expecting a block: { ... }")
            }
            catchBlock.done(KtNodeTypes.CATCH)
        }

        if (atWithRemap(KtTokens.FINALLY_KEYWORD)) {
            catchOrFinally = true
            val finallyBlock = mark()

            advance() // FINALLY_KEYWORD

            kotlinParsing.parseBlock()

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
        require(at(IF_KEYWORD))

        val marker = mark()

        advance() //IF_KEYWORD

        parseCondition()

        val thenBranch = mark()
        if (!at(ELSE_KEYWORD) && !at(KtTokens.SEMICOLON)) {
            parseControlStructureBody()
        }
        if (at(KtTokens.SEMICOLON) && lookahead(1) === ELSE_KEYWORD) {
            advance() // SEMICOLON
        }
        thenBranch.done(KtNodeTypes.THEN)

        // lookahead for arrow is needed to prevent capturing of whenEntry like "else -> "
        if (at(ELSE_KEYWORD) && lookahead(1) !== KtTokens.ARROW) {
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
        builder.disableNewlines()

        if (expect(KtTokens.LPAR, "Expecting a condition in parentheses '(...)'", EXPRESSION_FIRST)) {
            val condition = mark()
            parseExpression()
            condition.done(KtNodeTypes.CONDITION)
            expect(KtTokens.RPAR, "Expecting ')")
        }

        builder.restoreNewlinesState()
    }

    /*
     * : "continue" getEntryPoint?
     * : "break" getEntryPoint?
     */
    private fun parseJump(type: SyntaxElementType) {
        require(at(BREAK_KEYWORD) || at(CONTINUE_KEYWORD))

        val marker = mark()

        advance() // BREAK_KEYWORD or CONTINUE_KEYWORD

        parseLabelReferenceWithNoWhitespace()

        marker.done(type)
    }

    /*
     * "return" getEntryPoint? element?
     */
    private fun parseReturn() {
        require(at(RETURN_KEYWORD))

        val returnExpression = mark()

        advance() // RETURN_KEYWORD

        parseLabelReferenceWithNoWhitespace()

        if (atSetWithRemap(EXPRESSION_FIRST) && !at(KtTokens.EOL_OR_SEMICOLON)) parseExpression()

        returnExpression.done(KtNodeTypes.RETURN)
    }

    /*
     * labelReference?
     */
    private fun parseLabelReferenceWithNoWhitespace() {
        if (at(KtTokens.AT) && !builder.newlineBeforeCurrentToken()) {
            if (KtTokens.WHITE_SPACE_OR_COMMENT_BIT_SET.contains(builder.rawLookup(-1))) {
                error("There should be no space or comments before '@' in label reference")
            }
            parseLabelReference()
        }
    }

    /*
     * IDENTIFIER "@"
     */
    fun parseLabelDefinition() {
        require(this.isAtLabelDefinitionOrMissingIdentifier) { "Callers must check that current token is IDENTIFIER followed with '@'" }

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
        require(at(KtTokens.AT))

        val labelWrap = mark()

        val mark = mark()

        if (builder.rawLookup(1) !== KtTokens.IDENTIFIER) {
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
        require(at(THROW_KEYWORD))

        val marker = mark()

        advance() // THROW_KEYWORD

        parseExpression()

        marker.done(KtNodeTypes.THROW)
    }

    /*
     * "(" expression ")"
     */
    private fun parseParenthesizedExpression() {
        require(at(KtTokens.LPAR))

        val mark = mark()

        builder.disableNewlines()
        advance() // LPAR
        if (at(KtTokens.RPAR)) {
            error("Expecting an expression")
        } else {
            parseExpression()
        }

        expect(KtTokens.RPAR, "Expecting ')'")
        builder.restoreNewlinesState()

        mark.done(KtNodeTypes.PARENTHESIZED)
    }

    /*
     * "this" label?
     */
    private fun parseThisExpression() {
        require(at(THIS_KEYWORD))
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
        require(at(SUPER_KEYWORD))
        val mark = mark()

        val superReference = mark()
        advance() // SUPER_KEYWORD
        superReference.done(KtNodeTypes.REFERENCE_EXPRESSION)

        if (at(KtTokens.LT)) {
            // This may be "super < foo" or "super<foo>", thus the backtracking
            val supertype = mark()

            builder.disableNewlines()
            advance() // LT

            kotlinParsing.parseTypeRef()

            if (at(KtTokens.GT)) {
                advance() // GT
                supertype.drop()
            } else {
                supertype.rollbackTo()
            }
            builder.restoreNewlinesState()
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

        builder.disableNewlines()

        if (expect(KtTokens.LPAR, "Expecting an argument list", EXPRESSION_FOLLOW)) {
            if (!at(KtTokens.RPAR)) {
                while (true) {
                    while (at(KtTokens.COMMA)) {
                        errorAndAdvance("Expecting an argument")
                    }
                    parseValueArgument()
                    if (at(KtTokens.COLON) && lookahead(1) === KtTokens.IDENTIFIER) {
                        errorAndAdvance("Unexpected type specification", 2)
                    }
                    if (!at(KtTokens.COMMA)) {
                        if (atSetWithRemap(EXPRESSION_FIRST)) {
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

        builder.restoreNewlinesState()

        list.done(KtNodeTypes.VALUE_ARGUMENT_LIST)
    }

    /*
     * (SimpleName "=")? "*"? element
     */
    private fun parseValueArgument() {
        val argument = mark()
        if (atWithRemap(KtTokens.IDENTIFIER) && lookahead(1) === KtTokens.EQ) {
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
        kotlinParsing.parseObject(KotlinParsing.NameParsingMode.PROHIBITED, false) // Body is not optional because of foo(object : A, B)
        declaration.done(KtNodeTypes.OBJECT_DECLARATION)
        literal.done(KtNodeTypes.OBJECT_LITERAL)
    }

    private fun parseOneTokenExpression(type: SyntaxElementType) {
        val mark = mark()
        advance()
        mark.done(type)
    }

    override fun create(builder: SemanticWhitespaceAwareSyntaxBuilder): KotlinParsing {
        return kotlinParsing.create(builder)
    }

    private fun interruptedWithNewLine(): Boolean {
        return !ALLOW_NEWLINE_OPERATIONS.contains(tt()) && builder.newlineBeforeCurrentToken()
    }
}