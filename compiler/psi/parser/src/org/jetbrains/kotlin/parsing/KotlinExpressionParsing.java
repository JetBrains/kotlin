/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parsing;

import com.google.common.collect.ImmutableMap;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.lang.PsiBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.lang.BinaryOperationPrecedence;
import org.jetbrains.kotlin.lexer.KtToken;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.parsing.KotlinParsing.NameParsingMode;
import org.jetbrains.kotlin.psi.stubs.elements.KtTokenSets;

import static org.jetbrains.kotlin.KtNodeTypes.*;
import static org.jetbrains.kotlin.lang.BinaryOperationPrecedence.TOKEN_TO_BINARY_PRECEDENCE_MAP_WITH_SOFT_IDENTIFIERS;
import static org.jetbrains.kotlin.lexer.KtTokens.*;
import static org.jetbrains.kotlin.parsing.KotlinParsing.*;
import static org.jetbrains.kotlin.parsing.KotlinParsing.AnnotationParsingMode.DEFAULT;
import static org.jetbrains.kotlin.parsing.KotlinWhitespaceAndCommentsBindersKt.PRECEDING_ALL_COMMENTS_BINDER;
import static org.jetbrains.kotlin.parsing.KotlinWhitespaceAndCommentsBindersKt.TRAILING_ALL_COMMENTS_BINDER;

public class KotlinExpressionParsing extends AbstractKotlinParsing {
    private static final TokenSet WHEN_CONDITION_RECOVERY_SET = TokenSet.create(RBRACE, IN_KEYWORD, NOT_IN, IS_KEYWORD, NOT_IS, ELSE_KEYWORD);
    private static final TokenSet WHEN_CONDITION_RECOVERY_SET_WITH_ARROW = TokenSet.create(RBRACE, IN_KEYWORD, NOT_IN, IS_KEYWORD, NOT_IS, ELSE_KEYWORD, ARROW, DOT);
    private static final ImmutableMap<String, KtToken> KEYWORD_TEXTS = tokenSetToMap(KEYWORDS);

    private static final TokenSet TOKEN_SET_TO_FOLLOW_AFTER_DESTRUCTURING_DECLARATION_IN_LAMBDA = TokenSet.create(ARROW, COMMA, COLON);
    private static final TokenSet TOKEN_SET_TO_FOLLOW_AFTER_DESTRUCTURING_DECLARATION_IN_LAMBDA_RECOVERY =
            TokenSet.orSet(TOKEN_SET_TO_FOLLOW_AFTER_DESTRUCTURING_DECLARATION_IN_LAMBDA, PARAMETER_NAME_RECOVERY_SET);
    private static final TokenSet EQ_RPAR_SET = TokenSet.create(EQ, RPAR);
    private static final TokenSet ARROW_SET = TokenSet.create(ARROW);
    private static final TokenSet ARROW_COMMA_SET = TokenSet.create(ARROW, COMMA);
    private static final TokenSet IN_KEYWORD_R_PAR_COLON_SET = TokenSet.create(IN_KEYWORD, RPAR, COLON);
    private static final TokenSet IN_KEYWORD_L_BRACE_SET = TokenSet.create(IN_KEYWORD, LBRACE);
    private static final TokenSet IN_KEYWORD_L_BRACE_RECOVERY_SET = TokenSet.orSet(IN_KEYWORD_L_BRACE_SET, PARAMETER_NAME_RECOVERY_SET);
    private static final TokenSet COLON_IN_KEYWORD_SET = TokenSet.create(COLON, IN_KEYWORD);
    private static final TokenSet L_PAR_L_BRACE_R_PAR_SET = TokenSet.create(LPAR, LBRACE, RPAR);
    private static final TokenSet IN_KEYWORD_SET = TokenSet.create(IN_KEYWORD);
    private static final TokenSet TRY_CATCH_RECOVERY_TOKEN_SET = TokenSet.create(LBRACE, RBRACE, FINALLY_KEYWORD, CATCH_KEYWORD);

    private static ImmutableMap<String, KtToken> tokenSetToMap(TokenSet tokens) {
        ImmutableMap.Builder<String, KtToken> builder = ImmutableMap.builder();
        for (IElementType token : tokens.getTypes()) {
            builder.put(token.toString(), (KtToken) token);
        }
        return builder.build();
    }

    private static final TokenSet TYPE_ARGUMENT_LIST_STOPPERS = TokenSet.create(
            INTEGER_LITERAL, FLOAT_LITERAL, CHARACTER_LITERAL, INTERPOLATION_PREFIX, OPEN_QUOTE,
            PACKAGE_KEYWORD, AS_KEYWORD, TYPE_ALIAS_KEYWORD, INTERFACE_KEYWORD, CLASS_KEYWORD, THIS_KEYWORD, VAL_KEYWORD, VAR_KEYWORD,
            FUN_KEYWORD, FOR_KEYWORD, NULL_KEYWORD,
            TRUE_KEYWORD, FALSE_KEYWORD, IS_KEYWORD, THROW_KEYWORD, RETURN_KEYWORD, BREAK_KEYWORD,
            CONTINUE_KEYWORD, OBJECT_KEYWORD, IF_KEYWORD, TRY_KEYWORD, ELSE_KEYWORD, WHILE_KEYWORD, DO_KEYWORD,
            WHEN_KEYWORD, RBRACKET, RBRACE, RPAR, PLUSPLUS, MINUSMINUS, EXCLEXCL,
            //            MUL,
            PLUS, MINUS, EXCL, DIV, PERC, LTEQ,
            // TODO GTEQ,   foo<bar, baz>=x
            EQEQEQ, EXCLEQEQEQ, EQEQ, EXCLEQ, ANDAND, OROR, SAFE_ACCESS, ELVIS,
            SEMICOLON, RANGE, RANGE_UNTIL, EQ, MULTEQ, DIVEQ, PERCEQ, PLUSEQ, MINUSEQ, NOT_IN, NOT_IS,
            COLONCOLON,
            COLON
    );

    @SuppressWarnings("WeakerAccess")
    public static final TokenSet STATEMENT_FIRST = TokenSet.orSet(
            EXPRESSION_FIRST,
            TokenSet.create(
                    // declaration
                    FUN_KEYWORD,
                    VAL_KEYWORD, VAR_KEYWORD,
                    INTERFACE_KEYWORD,
                    CLASS_KEYWORD,
                    TYPE_ALIAS_KEYWORD
            ),
            MODIFIER_KEYWORDS
    );

    private static final TokenSet STATEMENT_NEW_LINE_QUICK_RECOVERY_SET =
            TokenSet.orSet(
                    TokenSet.andSet(STATEMENT_FIRST, TokenSet.andNot(KEYWORDS, TokenSet.create(IN_KEYWORD))),
                    TokenSet.create(EOL_OR_SEMICOLON));

    private static final BinaryOperationPrecedence MIN_BINARY_OPERATION_PRECEDENCE =
            BinaryOperationPrecedence.getEntries().get(0);

    private static final BinaryOperationPrecedence MAX_BINARY_OPERATION_PRECEDENCE =
            CollectionsKt.last(BinaryOperationPrecedence.getEntries());

    private static final TokenSet ALLOW_NEWLINE_OPERATIONS = TokenSet.create(
            DOT, SAFE_ACCESS,
            COLON, AS_KEYWORD, AS_SAFE,
            ELVIS,
            // Can't allow `is` and `!is` because of when entry conditions: IS_KEYWORD, NOT_IS,
            ANDAND,
            OROR
    );

    private final KotlinParsing myKotlinParsing;

    public KotlinExpressionParsing(SemanticWhitespaceAwarePsiBuilder builder, KotlinParsing kotlinParsing) {
        this(builder, kotlinParsing, true);
    }

    public KotlinExpressionParsing(SemanticWhitespaceAwarePsiBuilder builder, KotlinParsing kotlinParsing, boolean isLazy) {
        super(builder, isLazy);
        myKotlinParsing = kotlinParsing;
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
    public void parseExpression() {
        if (!atSet(EXPRESSION_FIRST)) {
            error("Expecting an expression");
            return;
        }

        parseBinaryExpression(MAX_BINARY_OPERATION_PRECEDENCE);
    }

    /**
     * <tt></T>element (operation element)*</tt>
     *
     * @return minPrecedence that should be considered during further parsing.
     * It's actual for <tt>IS</tt> operation because it's handled in a special way since it doesn't use recursive parsing on RHS
     */
    private BinaryOperationPrecedence parseBinaryExpression(BinaryOperationPrecedence maxPrecedence) {
        if (maxPrecedence == null) {
            throw new IllegalArgumentException("Shouldn't be here");
        }

        PsiBuilder.Marker expression = mark();

        parsePrefixExpression();

        BinaryOperationPrecedence minPrecedence = MIN_BINARY_OPERATION_PRECEDENCE;

        while (!interruptedWithNewLine()) {
            IElementType token = tt();

            KtToken operation = token instanceof KtToken ? (KtToken) token : null;
            if (operation == null) break;

            /*
                Match all allowed operations with the current or higher priority (lower precedence) at once.
                For instance, if the precedence is COMPARISON, we can match ADDITIVE, MULTIPLICATIVE but cannot match CONJUNCTION.
                The lowest priority is ASSIGNMENT.

                If the map doesn't contain the given token, the further parsing is not performed. Later that token either will be handled
                by an outer 'parseBinaryExpression()' call with a lower binary priority (higher precedence) or will not be handled at all
                if the expression is not binary.

                All Kotlin binary operations are syntactically left-associative.
                That's why we always use higher priority on recursive `parseBinaryExpression` call.

                If a right-associative operation is added, the same priority instead of the highest should be passed to the recursive call.
            */
            BinaryOperationPrecedence nextPrecedence = TOKEN_TO_BINARY_PRECEDENCE_MAP_WITH_SOFT_IDENTIFIERS.get(token);
            if (nextPrecedence == null ||
                nextPrecedence.ordinal() > maxPrecedence.ordinal() || nextPrecedence.ordinal() < minPrecedence.ordinal()) {
                break;
            }

            if (nextPrecedence == BinaryOperationPrecedence.INFIX && SOFT_KEYWORDS.contains(operation)) {
                // Remap soft keywords and modifiers that are treated as infix functions since there is no mutating `atSetWithRemap` call
                myBuilder.remapCurrentToken(KtTokens.IDENTIFIER);
            }

            parseOperationReference();

            IElementType resultType;
            switch (operation.tokenId) {
                case AS_KEYWORD_Id:
                case AS_SAFE_Id:
                    myKotlinParsing.parseTypeRefWithoutIntersections();
                    minPrecedence = BinaryOperationPrecedence.AS;
                    resultType = KtNodeTypes.BINARY_WITH_TYPE;
                    break;
                case IS_KEYWORD_Id:
                case NOT_IS_Id:
                    myKotlinParsing.parseTypeRefWithoutIntersections();
                    // The handling of `is`, it doesn't parse RHS recursively and greedily.
                    // To prevent parsing of more prioritized operations next to `is` (for instance, INFIX, RANGE, and others),
                    // use the `minPrecedence` in addition to `maxPrecedence`.
                    minPrecedence = BinaryOperationPrecedence.IN_OR_IS;
                    resultType = KtNodeTypes.IS_EXPRESSION;
                    break;
                default:
                    // Parse operations with higher priorities greedily.
                    minPrecedence = parseBinaryExpression(nextPrecedence.getHigherPriority());
                    resultType = KtNodeTypes.BINARY_EXPRESSION;
                    break;
            }

            expression.done(resultType);
            expression = expression.precede();
        }

        expression.drop();

        return minPrecedence;
    }

    /*
     * label prefixExpression
     */
    private void parseLabeledExpression() {
        PsiBuilder.Marker expression = mark();
        parseLabelDefinition();
        parsePrefixExpression();
        expression.done(LABELED_EXPRESSION);
    }

    /*
     * operation? prefixExpression
     */
    private void parsePrefixExpression() {
        if (at(AT)) {
            if (!parseLocalDeclaration(/* rollbackIfDefinitelyNotExpression = */ false, false)) {
                PsiBuilder.Marker expression = mark();
                myKotlinParsing.parseAnnotations(DEFAULT);
                parsePrefixExpression();
                expression.done(ANNOTATED_EXPRESSION);
            }
        }
        else {
            myBuilder.disableJoiningComplexTokens();
            if (isAtLabelDefinitionOrMissingIdentifier()) {
                myBuilder.restoreJoiningComplexTokensState();
                parseLabeledExpression();
            }
            else if (atSet(KtTokenSets.PREFIX_OPERATIONS)) {
                PsiBuilder.Marker expression = mark();

                parseOperationReference();

                myBuilder.restoreJoiningComplexTokensState();

                parsePrefixExpression();
                expression.done(PREFIX_EXPRESSION);
            }
            else {
                myBuilder.restoreJoiningComplexTokensState();
                parsePostfixExpression();
            }
        }
    }

    /*
     * doubleColonSuffix
     *   : "::" SimpleName typeArguments?
     *   ;
     */
    private boolean parseDoubleColonSuffix(@NotNull PsiBuilder.Marker expression) {
        if (!at(COLONCOLON)) return false;

        advance(); // COLONCOLON

        if (at(CLASS_KEYWORD)) {
            advance(); // CLASS_KEYWORD

            expression.done(CLASS_LITERAL_EXPRESSION);
            return true;
        }

        parseSimpleNameExpression();

        if (at(LT)) {
            PsiBuilder.Marker typeArgumentList = mark();
            if (myKotlinParsing.tryParseTypeArgumentList(TYPE_ARGUMENT_LIST_STOPPERS)) {
                typeArgumentList.error("Type arguments are not allowed");
            }
            else {
                typeArgumentList.rollbackTo();
            }
        }

        if (at(LPAR) && !myBuilder.newlineBeforeCurrentToken()) {
            PsiBuilder.Marker lpar = mark();
            parseCallSuffix();
            lpar.error("This syntax is reserved for future use; to call a reference, enclose it in parentheses: (foo::bar)(args)");
        }

        expression.done(CALLABLE_REFERENCE_EXPRESSION);
        return true;
    }

    private void skipQuestionMarksBeforeDoubleColon() {
        if (at(QUEST)) {
            int k = 1;
            while (lookahead(k) == QUEST) k++;
            if (lookahead(k) == COLONCOLON) {
                while (k > 0) {
                    advance(); // QUEST
                    k--;
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
    private void parsePostfixExpression() {
        PsiBuilder.Marker expression = mark();

        boolean firstExpressionParsed = at(COLONCOLON) ? parseDoubleColonSuffix(mark()) : parseAtomicExpression();

        while (true) {
            if (interruptedWithNewLine()) {
                break;
            }
            else if (at(LBRACKET)) {
                parseArrayAccess();
                expression.done(ARRAY_ACCESS_EXPRESSION);
            }
            else if (parseCallSuffix()) {
                expression.done(CALL_EXPRESSION);
            }
            else if (at(DOT) || at(SAFE_ACCESS)) {
                IElementType expressionType = at(DOT) ? DOT_QUALIFIED_EXPRESSION : SAFE_ACCESS_EXPRESSION;
                advance(); // DOT or SAFE_ACCESS

                if (!firstExpressionParsed) {
                    expression.drop();
                    expression = mark();
                    firstExpressionParsed = parseAtomicExpression();
                    continue;
                }

                parseSelectorCallExpression();

                expression.done(expressionType);
            }
            else if (atSet(KtTokenSets.POSTFIX_OPERATIONS)) {
                parseOperationReference();
                expression.done(POSTFIX_EXPRESSION);
            }
            else {
                skipQuestionMarksBeforeDoubleColon();
                if (!parseDoubleColonSuffix(expression)) {
                    break;
                }
            }
            expression = expression.precede();
        }
        expression.drop();
    }

    /*
     * callSuffix
     *   : typeArguments? valueArguments annotatedLambda
     *   : typeArguments annotatedLambda
     *   ;
     */
    private boolean parseCallSuffix() {
        if (parseCallWithClosure()) {
            // do nothing
        }
        else if (at(LPAR)) {
            parseValueArgumentList();
            parseCallWithClosure();
        }
        else if (at(LT)) {
            PsiBuilder.Marker typeArgumentList = mark();
            if (myKotlinParsing.tryParseTypeArgumentList(TYPE_ARGUMENT_LIST_STOPPERS)) {
                typeArgumentList.done(TYPE_ARGUMENT_LIST);
                if (!myBuilder.newlineBeforeCurrentToken() && at(LPAR)) parseValueArgumentList();
                parseCallWithClosure();
            }
            else {
                typeArgumentList.rollbackTo();
                return false;
            }
        }
        else {
            return false;
        }

        return true;
    }

    /*
     * atomicExpression typeParameters? valueParameters? functionLiteral*
     */
    private void parseSelectorCallExpression() {
        PsiBuilder.Marker mark = mark();
        parseAtomicExpression();
        if (!myBuilder.newlineBeforeCurrentToken() && parseCallSuffix()) {
            mark.done(CALL_EXPRESSION);
        }
        else {
            mark.drop();
        }
    }

    private void parseOperationReference() {
        PsiBuilder.Marker operationReference = mark();
        advance(); // operation
        operationReference.done(OPERATION_REFERENCE);
    }

    /*
     * annotatedLambda*
     */
    protected boolean parseCallWithClosure() {
        boolean success = false;

        while (true) {
            PsiBuilder.Marker argument = mark();

            if (!parseAnnotatedLambda(/* preferBlock = */false)) {
                argument.drop();
                break;
            }

            argument.done(LAMBDA_ARGUMENT);
            success = true;
        }

        return success;
    }

    /*
     * annotatedLambda
     *  : ("@" annotationEntry)* labelDefinition? functionLiteral
     */
    private boolean parseAnnotatedLambda(boolean preferBlock) {
        PsiBuilder.Marker annotated = mark();

        boolean wereAnnotations = myKotlinParsing.parseAnnotations(DEFAULT);
        PsiBuilder.Marker labeled = mark();

        boolean wasLabel = isAtLabelDefinitionOrMissingIdentifier();
        if (wasLabel) {
            parseLabelDefinition();
        }

        if (!at(LBRACE)) {
            annotated.rollbackTo();
            return false;
        }

        parseFunctionLiteral(preferBlock, /* collapse = */true);

        doneOrDrop(labeled, LABELED_EXPRESSION, wasLabel);
        doneOrDrop(annotated, ANNOTATED_EXPRESSION, wereAnnotations);

        return true;
    }

    private static void doneOrDrop(
            @NotNull PsiBuilder.Marker marker,
            @NotNull IElementType type,
            boolean condition
    ) {
        if (condition) {
            marker.done(type);
        }
        else {
            marker.drop();
        }
    }

    boolean isAtLabelDefinitionOrMissingIdentifier() {
        return (at(IDENTIFIER) && myBuilder.rawLookup(1) == AT) || at(AT);
    }

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
    private boolean parseAtomicExpression() {
        boolean ok = true;

        switch (getTokenId()) {
            case LPAR_Id:
                parseParenthesizedExpression();
                break;
            case LBRACKET_Id:
                parseCollectionLiteralExpression();
                break;
            case THIS_KEYWORD_Id:
                parseThisExpression();
                break;
            case SUPER_KEYWORD_Id:
                parseSuperExpression();
                break;
            case OBJECT_KEYWORD_Id:
                parseObjectLiteral();
                break;
            case THROW_KEYWORD_Id:
                parseThrow();
                break;
            case RETURN_KEYWORD_Id:
                parseReturn();
                break;
            case CONTINUE_KEYWORD_Id:
                parseJump(CONTINUE);
                break;
            case BREAK_KEYWORD_Id:
                parseJump(BREAK);
                break;
            case IF_KEYWORD_Id:
                parseIf();
                break;
            case WHEN_KEYWORD_Id:
                parseWhen();
                break;
            case TRY_KEYWORD_Id:
                parseTry();
                break;
            case FOR_KEYWORD_Id:
                parseFor();
                break;
            case WHILE_KEYWORD_Id:
                parseWhile();
                break;
            case DO_KEYWORD_Id:
                parseDoWhile();
                break;
            case IDENTIFIER_Id:
                // Try to parse anonymous function with context parameters
                if (at(CONTEXT_KEYWORD) && lookahead(1) == LPAR) {
                    if (parseLocalDeclaration(true, false)) {
                        break;
                    } else {
                        at(IDENTIFIER);
                    }
                }

                parseSimpleNameExpression();
                break;
            case LBRACE_Id:
                parseFunctionLiteral();
                break;
            case INTERPOLATION_PREFIX_Id:
            case OPEN_QUOTE_Id:
                parseStringTemplate();
                break;
            /*
             * literalConstant
             *   : "true" | "false"
             *   : stringTemplate
             *   : NoEscapeString
             *   : IntegerLiteral
             *   : CharacterLiteral
             *   : FloatLiteral
             *   : "null"
             *   ;
             */
            case TRUE_KEYWORD_Id:
            case FALSE_KEYWORD_Id:
                parseOneTokenExpression(BOOLEAN_CONSTANT);
                break;
            case INTEGER_LITERAL_Id:
                parseOneTokenExpression(INTEGER_CONSTANT);
                break;
            case CHARACTER_LITERAL_Id:
                parseOneTokenExpression(CHARACTER_CONSTANT);
                break;
            case FLOAT_LITERAL_Id:
                parseOneTokenExpression(FLOAT_CONSTANT);
                break;
            case NULL_KEYWORD_Id:
                parseOneTokenExpression(NULL);
                break;
            case CLASS_KEYWORD_Id:
            case INTERFACE_KEYWORD_Id:
            case FUN_KEYWORD_Id:
            case VAL_KEYWORD_Id:
            case VAR_KEYWORD_Id:
            case TYPE_ALIAS_KEYWORD_Id:
                if (!parseLocalDeclaration(/* rollbackIfDefinitelyNotExpression = */ myBuilder.newlineBeforeCurrentToken(), false)) {
                    ok = false;
                }
                // declaration was parsed, do nothing
                break;
            default:
                ok = false;
        }

        if (!ok) {
            // TODO: better recovery if FIRST(element) did not match
            errorWithRecovery("Expecting an element", TokenSet.orSet(EXPRESSION_FOLLOW, TokenSet.create(LONG_TEMPLATE_ENTRY_END)));
        }

        return ok;
    }

    /*
     * stringTemplate
     *   : INTERPOLATION_PREFIX OPEN_QUOTE stringTemplateElement* CLOSING_QUOTE
     *   ;
     */
    private void parseStringTemplate() {
        assert _at(INTERPOLATION_PREFIX) || _at(OPEN_QUOTE);

        PsiBuilder.Marker template = mark();

        if (at(INTERPOLATION_PREFIX)) {
            PsiBuilder.Marker mark = mark();
            advance(); // INTERPOLATION_PREFIX
            mark.done(STRING_INTERPOLATION_PREFIX);
        }

        assert _at(OPEN_QUOTE);
        advance(); // OPEN_QUOTE

        while (!eof()) {
            if (at(CLOSING_QUOTE) || at(DANGLING_NEWLINE)) {
                break;
            }
            parseStringTemplateElement();
        }

        if (at(DANGLING_NEWLINE)) {
            errorAndAdvance("Expecting '\"'");
        }
        else {
            expect(CLOSING_QUOTE, "Expecting '\"'");
        }
        template.done(STRING_TEMPLATE);
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
    private void parseStringTemplateElement() {
        if (at(REGULAR_STRING_PART)) {
            PsiBuilder.Marker mark = mark();
            advance(); // REGULAR_STRING_PART
            mark.done(LITERAL_STRING_TEMPLATE_ENTRY);
        }
        else if (at(ESCAPE_SEQUENCE)) {
            PsiBuilder.Marker mark = mark();
            advance(); // ESCAPE_SEQUENCE
            mark.done(ESCAPE_STRING_TEMPLATE_ENTRY);
        }
        else if (at(SHORT_TEMPLATE_ENTRY_START)) {
            PsiBuilder.Marker entry = mark();
            advance(); // SHORT_TEMPLATE_ENTRY_START

            if (at(THIS_KEYWORD)) {
                PsiBuilder.Marker thisExpression = mark();
                PsiBuilder.Marker reference = mark();
                advance(); // THIS_KEYWORD
                reference.done(REFERENCE_EXPRESSION);
                thisExpression.done(THIS_EXPRESSION);
            }
            else {
                KtToken keyword = KEYWORD_TEXTS.get(myBuilder.getTokenText());
                if (keyword != null) {
                    myBuilder.remapCurrentToken(keyword);
                    errorAndAdvance("Keyword cannot be used as a reference");
                }
                else {
                    PsiBuilder.Marker reference = mark();
                    expect(IDENTIFIER, "Expecting a name");
                    reference.done(REFERENCE_EXPRESSION);
                }
            }

            entry.done(SHORT_STRING_TEMPLATE_ENTRY);
        }
        else if (at(LONG_TEMPLATE_ENTRY_START)) {
            PsiBuilder.Marker longTemplateEntry = mark();

            advance(); // LONG_TEMPLATE_ENTRY_START

            while (!eof()) {
                int offset = myBuilder.getCurrentOffset();

                parseExpression();

                if (_at(LONG_TEMPLATE_ENTRY_END)) {
                    advance();
                    break;
                }
                else {
                    error("Expecting '}'");
                    if (offset == myBuilder.getCurrentOffset()) {
                        // Prevent hang if can't advance with parseExpression()
                        advance();
                    }
                }
            }

            longTemplateEntry.done(LONG_STRING_TEMPLATE_ENTRY);
        }
        else {
            errorAndAdvance("Unexpected token in a string template");
        }
    }

    /*
     * when
     *   : "when" ("(" (modifiers "val" SimpleName "=")? element ")")? "{"
     *         whenEntry*
     *     "}"
     *   ;
     */
    private void parseWhen() {
        assert _at(WHEN_KEYWORD);

        PsiBuilder.Marker when = mark();

        advance(); // WHEN_KEYWORD

        // Parse condition
        myBuilder.disableNewlines();
        if (at(LPAR)) {
            advanceAt(LPAR);

            PsiBuilder.Marker atWhenStart = mark();
            myKotlinParsing.parseAnnotationsList(EQ_RPAR_SET);
            if (at(VAL_KEYWORD) || at(VAR_KEYWORD)) {
                IElementType declType = myKotlinParsing.parseProperty(KotlinParsing.DeclarationParsingMode.LOCAL);

                atWhenStart.done(declType);
                atWhenStart.setCustomEdgeTokenBinders(PrecedingDocCommentsBinder.INSTANCE, TrailingCommentsBinder.INSTANCE);
            }
            else {
                atWhenStart.drop();
                parseExpression();
            }

            expect(RPAR, "Expecting ')'");
        }
        myBuilder.restoreNewlinesState();

        // Parse when block
        myBuilder.enableNewlines();
        if (expect(LBRACE, "Expecting '{'")) {
            while (!eof() && !at(RBRACE)) {
                parseWhenEntry();
            }

            expect(RBRACE, "Expecting '}'");
        }
        myBuilder.restoreNewlinesState();

        when.done(WHEN);
    }

    /*
     * whenEntry
     *   // TODO : consider empty after ->
     *   : whenCondition{","} whenEntryGuard? "->" element SEMI
     *   : "else" whenEntryGuard? "->" element SEMI
     *   ;
     */
    private void parseWhenEntry() {
        PsiBuilder.Marker entry = mark();

        if (at(ELSE_KEYWORD)) {
            advance(); // ELSE_KEYWORD

            parseWhenEntryGuardOrSuggest();

            if (!at(ARROW)) {
                errorUntil("Expecting '->'", TokenSet.create(ARROW, LBRACE, RBRACE, EOL_OR_SEMICOLON));
            }

            if (at(ARROW)) {
                advance(); // ARROW

                if (atSet(WHEN_CONDITION_RECOVERY_SET)) {
                    error("Expecting an element");
                }
                else {
                    parseControlStructureBody();
                }
            }
            else if (at(LBRACE)) { // no arrow, probably it's simply missing
                parseControlStructureBody();
            }
            else if (!atSet(WHEN_CONDITION_RECOVERY_SET)) {
                errorAndAdvance("Expecting '->'");
            }
        }
        else {
            parseWhenEntryNotElse();
        }

        entry.done(WHEN_ENTRY);
        consumeIf(SEMICOLON);
    }

    /*
     * : whenCondition{","} whenEntryGuard? "->" element SEMI
     */
    private void parseWhenEntryNotElse() {
        while (true) {
            while (at(COMMA)) errorAndAdvance("Expecting a when-condition");
            parseWhenCondition();
            if (!at(COMMA)) break;
            advance(); // COMMA
            if (at(ARROW)) {
                break;
            }
        }

        parseWhenEntryGuardOrSuggest();

        expect(ARROW, "Expecting '->'", WHEN_CONDITION_RECOVERY_SET);
        if (atSet(WHEN_CONDITION_RECOVERY_SET)) {
            error("Expecting an element");
        }
        else {
            parseControlStructureBody();
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
    private void parseWhenCondition() {
        PsiBuilder.Marker condition = mark();
        myBuilder.disableNewlines();
        switch (getTokenId()) {
            case IN_KEYWORD_Id:
            case NOT_IN_Id:
                PsiBuilder.Marker mark = mark();
                advance(); // IN_KEYWORD or NOT_IN
                mark.done(OPERATION_REFERENCE);


                if (atSet(WHEN_CONDITION_RECOVERY_SET_WITH_ARROW)) {
                    error("Expecting an element");
                }
                else {
                    parseExpression();
                }
                condition.done(WHEN_CONDITION_IN_RANGE);
                break;
            case IS_KEYWORD_Id:
            case NOT_IS_Id:
                advance(); // IS_KEYWORD or NOT_IS

                if (atSet(WHEN_CONDITION_RECOVERY_SET_WITH_ARROW)) {
                    error("Expecting a type");
                }
                else {
                    myKotlinParsing.parseTypeRef();
                }
                condition.done(WHEN_CONDITION_IS_PATTERN);
                break;
            case RBRACE_Id:
            case ELSE_KEYWORD_Id:
            case ARROW_Id:
            case DOT_Id:
                error("Expecting an expression, is-condition or in-condition");
                condition.done(WHEN_CONDITION_EXPRESSION);
                break;
            default:
                parseExpression();
                condition.done(WHEN_CONDITION_EXPRESSION);
                break;
        }
        myBuilder.restoreNewlinesState();
    }

    private void parseWhenEntryGuardOrSuggest() {
        if (at(ANDAND)) {
            errorUntil("Unexpected '&&', use 'if' to introduce additional conditions; see https://kotl.in/guards-in-when", TokenSet.create(LBRACE, RBRACE, ARROW));
        } else if (at(IF_KEYWORD)) {
            parseWhenEntryGuard();
        }
    }

    /*
     * whenEntryGuard
     *   : "if" expression
     *   ;
     */
    private void parseWhenEntryGuard() {
        assert _at(IF_KEYWORD);

        PsiBuilder.Marker guard = mark();
        advance(); // IF_KEYWORD
        parseExpression();
        guard.done(WHEN_ENTRY_GUARD);
    }

    /*
     * arrayAccess
     *   : "[" element{","} "]"
     *   ;
     */
    private void parseArrayAccess() {
        parseAsCollectionLiteralExpression(INDICES, false);
    }

    /*
     * collectionLiteral
     *   : "[" element{","}? "]"
     *   ;
     */
    private void parseCollectionLiteralExpression() {
        parseAsCollectionLiteralExpression(COLLECTION_LITERAL_EXPRESSION, true);
    }

    private void parseAsCollectionLiteralExpression(IElementType nodeType, boolean canBeEmpty) {
        assert _at(LBRACKET);

        PsiBuilder.Marker innerExpressions = mark();

        myBuilder.disableNewlines();
        advance(); // LBRACKET

        if (!canBeEmpty && at(RBRACKET)) {
            PsiBuilder.Marker empty = mark();
            empty.done(EMPTY_VALUE_ARGUMENT);
        }
        else {
            parseInnerExpressions();
        }

        expect(RBRACKET, "Expecting ']'");
        myBuilder.restoreNewlinesState();

        innerExpressions.done(nodeType);
    }

    private void parseInnerExpressions() {
        while (true) {
            if (at(COMMA)) {
                PsiBuilder.Marker empty = mark();
                empty.done(EMPTY_VALUE_ARGUMENT);
            } else if (at(RBRACKET)) {
                break;
            } else {
                parseExpression();
                if (!at(COMMA)) break;
            }

            advance(); // COMMA
        }
    }

    public void parseContractDescriptionBlock() {
        assert _at(CONTRACT_KEYWORD);

        advance(); // CONTRACT_KEYWORD

        parseContractEffectList();
    }

    private void parseContractEffectList() {
        PsiBuilder.Marker block = mark();

        expect(LBRACKET, "Expecting '['");
        myBuilder.enableNewlines();

        parseContractEffects();

        expect(RBRACKET, "Expecting ']'");
        myBuilder.restoreNewlinesState();

        block.done(CONTRACT_EFFECT_LIST);
    }

    private void parseContractEffects() {
        while (true) {
            if (at(COMMA)) errorAndAdvance("Expecting a contract effect");
            if (at(RBRACKET)) {
                break;
            }
            PsiBuilder.Marker effect = mark();
            parseExpression();
            effect.done(CONTRACT_EFFECT);

            if (!at(COMMA)) break;
            advance(); // COMMA
        }
    }

    /*
     * SimpleName
     */
    public void parseSimpleNameExpression() {
        PsiBuilder.Marker simpleName = mark();
        expect(IDENTIFIER, "Expecting an identifier");
        simpleName.done(REFERENCE_EXPRESSION);
    }

    /*
     * modifiers declarationRest
     */
    private boolean parseLocalDeclaration(boolean rollbackIfDefinitelyNotExpression, boolean isScriptTopLevel) {
        PsiBuilder.Marker decl = mark();
        KotlinParsing.ModifierDetector detector = new KotlinParsing.ModifierDetector();
        myKotlinParsing.parseModifierList(detector, TokenSet.EMPTY);

        IElementType declType = parseLocalDeclarationRest(detector, rollbackIfDefinitelyNotExpression, isScriptTopLevel);

        if (declType != null) {
            // we do not attach preceding comments (non-doc) to local variables because they are likely commenting a few statements below
            closeDeclarationWithCommentBinders(decl, declType,
                                               declType != KtNodeTypes.PROPERTY && declType != KtNodeTypes.DESTRUCTURING_DECLARATION);
            return true;
        }
        else {
            decl.rollbackTo();
            return false;
        }
    }

    /*
     * functionLiteral  // one can use "it" as a parameter name
     *   : "{" expressions "}"
     *   : "{" (modifiers SimpleName (":" type)?){","} "->" statements "}"
     *   ;
     */
    private void parseFunctionLiteral() {
        parseFunctionLiteral(/* preferBlock = */false, /* collapse = */true);
    }

    /**
     * If it has no ->, it's a block, otherwise a function literal
     *
     * Please update {@link org.jetbrains.kotlin.BlockExpressionElementType#isParsable(ASTNode, CharSequence, Language, Project)} if any changes occurs!
     */
    public void parseFunctionLiteral(boolean preferBlock, boolean collapse) {
        assert _at(LBRACE);

        PsiBuilder.Marker literalExpression = mark();

        PsiBuilder.Marker literal = mark();

        myBuilder.enableNewlines();
        advance(); // LBRACE

        boolean paramsFound = false;

        IElementType token = tt();
        if (token == ARROW) {
            //   { -> ...}
            mark().done(VALUE_PARAMETER_LIST);
            advance(); // ARROW
            paramsFound = true;
        }
        else if (token == IDENTIFIER || token == COLON || token == LPAR || token == LBRACKET) {
            // Try to parse a simple name list followed by an ARROW
            //   {a -> ...}
            //   {a, b -> ...}
            //   {(a, b) -> ... }
            PsiBuilder.Marker rollbackMarker = mark();
            IElementType nextToken = lookahead(1);
            boolean preferParamsToExpressions = (nextToken == COMMA || nextToken == COLON);
            parseFunctionLiteralParameterList();

            paramsFound = preferParamsToExpressions ?
                          rollbackOrDrop(rollbackMarker, ARROW, "An -> is expected", RBRACE) :
                          rollbackOrDropAt(rollbackMarker, ARROW);
        }

        if (!paramsFound && preferBlock) {
            literal.drop();
            parseStatements();
            expect(RBRACE, "Expecting '}'");
            literalExpression.done(BLOCK);
            myBuilder.restoreNewlinesState();

            return;
        }

        if (collapse && isLazy) {
            myKotlinParsing.advanceBalancedBlock();
            literal.done(FUNCTION_LITERAL);
            literalExpression.collapse(LAMBDA_EXPRESSION);
        }
        else {
            PsiBuilder.Marker body = mark();
            parseStatements();

            body.done(BLOCK);
            body.setCustomEdgeTokenBinders(PRECEDING_ALL_COMMENTS_BINDER, TRAILING_ALL_COMMENTS_BINDER);

            expect(RBRACE, "Expecting '}'");
            literal.done(FUNCTION_LITERAL);
            literalExpression.done(LAMBDA_EXPRESSION);
        }

        myBuilder.restoreNewlinesState();
    }

    private boolean rollbackOrDropAt(PsiBuilder.Marker rollbackMarker, IElementType dropAt) {
        if (at(dropAt)) {
            advance(); // dropAt
            rollbackMarker.drop();
            return true;
        }
        rollbackMarker.rollbackTo();
        return false;
    }

    private boolean rollbackOrDrop(PsiBuilder.Marker rollbackMarker,
            KtToken expected, String expectMessage,
            IElementType validForDrop) {
        if (at(expected)) {
            advance(); // dropAt
            rollbackMarker.drop();
            return true;
        }
        else if (at(validForDrop)) {
            rollbackMarker.drop();
            expect(expected, expectMessage);
            return true;
        }

        rollbackMarker.rollbackTo();
        return false;
    }


    /*
     * lambdaParameter{","}
     *
     * lambdaParameter
     *   : variableDeclarationEntry
     *   : multipleVariableDeclarations (":" type)?
     */
    private void parseFunctionLiteralParameterList() {
        PsiBuilder.Marker parameterList = mark();

        while (!eof()) {
            if (at(ARROW)) {
                break;
            }
            PsiBuilder.Marker parameter = mark();

            if (at(COLON)) {
                error("Expecting parameter name");
            }
            else if (at(LPAR) || at(LBRACKET)) {
                PsiBuilder.Marker destructuringDeclaration = mark();
                myKotlinParsing.parseMultiDeclarationEntry(
                        TOKEN_SET_TO_FOLLOW_AFTER_DESTRUCTURING_DECLARATION_IN_LAMBDA,
                        TOKEN_SET_TO_FOLLOW_AFTER_DESTRUCTURING_DECLARATION_IN_LAMBDA_RECOVERY,
                        // No var in lambda parameter destructuring
                        lookahead(1) == VAL_KEYWORD ? MultiDeclarationMode.FULL_VAL_ONLY : MultiDeclarationMode.SHORT);
                destructuringDeclaration.done(DESTRUCTURING_DECLARATION);
            }
            else {
                expect(IDENTIFIER, "Expecting parameter name", ARROW_SET);
            }

            if (at(COLON)) {
                advance(); // COLON
                myKotlinParsing.parseTypeRef(ARROW_COMMA_SET);
            }
            parameter.done(VALUE_PARAMETER);

            if (at(ARROW)) {
                break;
            }
            else if (at(COMMA)) {
                advance(); // COMMA
            }
            else {
                error("Expecting '->' or ','");
                break;
            }
        }

        parameterList.done(VALUE_PARAMETER_LIST);
    }

    /*
     * expressions
     *   : SEMI* statement{SEMI+} SEMI*
     */
    public void parseStatements() {
        parseStatements(false);
    }

    /*
         * expressions
         *   : SEMI* statement{SEMI+} SEMI*
         */
    public void parseStatements(boolean isScriptTopLevel) {
        while (at(SEMICOLON)) advance(); // SEMICOLON
        while (!eof() && !at(RBRACE)) {
            if (!atSet(STATEMENT_FIRST)) {
                errorAndAdvance("Expecting an element");
            }
            if (atSet(STATEMENT_FIRST)) {
                parseStatement(isScriptTopLevel);
            }
            if (at(SEMICOLON)) {
                while (at(SEMICOLON)) advance(); // SEMICOLON
            }
            else if (at(RBRACE)) {
                break;
            }
            else if (!isScriptTopLevel && !myBuilder.newlineBeforeCurrentToken()) {
                String severalStatementsError = "Unexpected tokens (use ';' to separate expressions on the same line)";

                if (atSet(STATEMENT_NEW_LINE_QUICK_RECOVERY_SET)) {
                    error(severalStatementsError);
                }
                else {
                    errorUntil(severalStatementsError, TokenSet.create(EOL_OR_SEMICOLON, LBRACE, RBRACE));
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
    private void parseStatement(boolean isScriptTopLevel) {
        if (!parseLocalDeclaration(/* rollbackIfDefinitelyNotExpression = */false, /* isScriptTopLevel = */ isScriptTopLevel)) {
            if (!atSet(EXPRESSION_FIRST)) {
                errorAndAdvance("Expecting a statement");
            }
            else if (isScriptTopLevel){
                PsiBuilder.Marker scriptInitializer = mark();
                parseBlockLevelExpression();
                scriptInitializer.done(SCRIPT_INITIALIZER);
            }
            else {
                parseBlockLevelExpression();
            }
        }
    }

    /*
     * blockLevelExpression
     *  : annotations + ("\n")+ expression
     *  ;
     */
    private void parseBlockLevelExpression() {
        if (at(AT)) {
            PsiBuilder.Marker expression = mark();
            myKotlinParsing.parseAnnotations(DEFAULT);

            if (!myBuilder.newlineBeforeCurrentToken()) {
                expression.rollbackTo();
                parseExpression();
                return;
            }

            parseBlockLevelExpression();
            expression.done(ANNOTATED_EXPRESSION);
            return;
        }

        parseExpression();
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
    @Nullable
    private IElementType parseLocalDeclarationRest(
            @NotNull KotlinParsing.ModifierDetector modifierDetector,
            boolean failIfDefinitelyNotExpression,
            boolean isScriptTopLevel
    ) {
        IElementType keywordToken = tt();
        if (failIfDefinitelyNotExpression) {
            if (keywordToken != FUN_KEYWORD) return null;

            return myKotlinParsing.parseFunction(/* failIfIdentifierExists = */ true);
        }

        if (keywordToken == OBJECT_KEYWORD) {
            // Object expression may appear at the statement position: should parse it
            // as expression instead of object declaration
            // sample:
            // {
            //   object : Thread() {
            //   }
            // }
            IElementType lookahead = lookahead(1);
            if (lookahead == COLON || lookahead == LBRACE) {
                return null;
            }
        }

        return myKotlinParsing.parseCommonDeclaration(
                modifierDetector, NameParsingMode.REQUIRED,
                isScriptTopLevel ? KotlinParsing.DeclarationParsingMode.SCRIPT_TOPLEVEL : KotlinParsing.DeclarationParsingMode.LOCAL
        );
    }

    /*
     * doWhile
     *   : "do" element "while" "(" element ")"
     *   ;
     */
    private void parseDoWhile() {
        assert _at(DO_KEYWORD);

        PsiBuilder.Marker loop = mark();

        advance(); // DO_KEYWORD

        if (!at(WHILE_KEYWORD)) {
            parseLoopBody();
        }

        if (expect(WHILE_KEYWORD, "Expecting 'while' followed by a post-condition")) {
            parseCondition();
        }

        loop.done(DO_WHILE);
    }

    /*
     * while
     *   : "while" "(" element ")" element
     *   ;
     */
    private void parseWhile() {
        assert _at(WHILE_KEYWORD);

        PsiBuilder.Marker loop = mark();

        advance(); // WHILE_KEYWORD

        parseCondition();

        parseLoopBody();

        loop.done(WHILE);
    }

    /*
     * for
     *   : "for" "(" annotations ("val" | "var")? (multipleVariableDeclarations | variableDeclarationEntry) "in" expression ")" expression
     *   ;
     *
     *   TODO: empty loop body (at the end of the block)?
     */
    private void parseFor() {
        assert _at(FOR_KEYWORD);

        PsiBuilder.Marker loop = mark();

        advance(); // FOR_KEYWORD

        if (expect(LPAR, "Expecting '(' to open a loop range", EXPRESSION_FIRST)) {
            myBuilder.disableNewlines();

            if (!at(RPAR)) {
                PsiBuilder.Marker parameter = mark();

                if (!at(IN_KEYWORD)) {
                    myKotlinParsing.parseModifierList(IN_KEYWORD_R_PAR_COLON_SET);
                }

                if (at(VAL_KEYWORD) || at(VAR_KEYWORD)) advance(); // VAL_KEYWORD or VAR_KEYWORD

                if (at(LPAR) || at(LBRACKET)) {
                    PsiBuilder.Marker destructuringDeclaration = mark();
                    myKotlinParsing.parseMultiDeclarationEntry(
                            IN_KEYWORD_L_BRACE_SET,
                            IN_KEYWORD_L_BRACE_RECOVERY_SET,
                            // No var in destructured loop parameter
                            lookahead(1) == VAL_KEYWORD ? MultiDeclarationMode.FULL_VAL_ONLY : MultiDeclarationMode.SHORT);
                    destructuringDeclaration.done(DESTRUCTURING_DECLARATION);
                }
                else {
                    expect(IDENTIFIER, "Expecting a variable name", COLON_IN_KEYWORD_SET);

                    if (at(COLON)) {
                        advance(); // COLON
                        myKotlinParsing.parseTypeRef(IN_KEYWORD_SET);
                    }
                }
                parameter.done(VALUE_PARAMETER);

                if (expect(IN_KEYWORD, "Expecting 'in'", L_PAR_L_BRACE_R_PAR_SET)) {
                    PsiBuilder.Marker range = mark();
                    parseExpression();
                    range.done(LOOP_RANGE);
                }
            }
            else {
                error("Expecting a variable name");
            }

            expectNoAdvance(RPAR, "Expecting ')'");
            myBuilder.restoreNewlinesState();
        }

        parseLoopBody();

        loop.done(FOR);
    }

    private void parseControlStructureBody() {
        if (!parseAnnotatedLambda(/* preferBlock = */true)) {
            parseBlockLevelExpression();
        }
    }

    /*
     * element
     */
    private void parseLoopBody() {
        PsiBuilder.Marker body = mark();
        if (!at(SEMICOLON)) {
            parseControlStructureBody();
        }
        body.done(BODY);
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
    private void parseTry() {
        assert _at(TRY_KEYWORD);

        PsiBuilder.Marker tryExpression = mark();

        advance(); // TRY_KEYWORD

        myKotlinParsing.parseBlock();

        boolean catchOrFinally = false;
        while (at(CATCH_KEYWORD)) {
            catchOrFinally = true;
            PsiBuilder.Marker catchBlock = mark();
            advance(); // CATCH_KEYWORD

            if (atSet(TRY_CATCH_RECOVERY_TOKEN_SET)) {
                error("Expecting exception variable declaration");
            }
            else {
                PsiBuilder.Marker parameters = mark();
                expect(LPAR, "Expecting '('", TRY_CATCH_RECOVERY_TOKEN_SET);
                if (!atSet(TRY_CATCH_RECOVERY_TOKEN_SET)) {
                    myKotlinParsing.parseValueParameter(/*typeRequired = */ true);
                    if (at(COMMA)) {
                        advance(); // trailing comma
                    }
                    expect(RPAR, "Expecting ')'", TRY_CATCH_RECOVERY_TOKEN_SET);
                }
                else {
                    error("Expecting exception variable declaration");
                }
                parameters.done(VALUE_PARAMETER_LIST);
            }

            if (at(LBRACE)) {
                myKotlinParsing.parseBlock();
            }
            else {
                error("Expecting a block: { ... }");
            }
            catchBlock.done(CATCH);
        }

        if (at(FINALLY_KEYWORD)) {
            catchOrFinally = true;
            PsiBuilder.Marker finallyBlock = mark();

            advance(); // FINALLY_KEYWORD

            myKotlinParsing.parseBlock();

            finallyBlock.done(FINALLY);
        }

        if (!catchOrFinally) {
            error("Expecting 'catch' or 'finally'");
        }

        tryExpression.done(TRY);
    }

    /*
     * if
     *   : "if" "(" element ")" element SEMI? ("else" element)?
     *   ;
     */
    private void parseIf() {
        assert _at(IF_KEYWORD);

        PsiBuilder.Marker marker = mark();

        advance(); //IF_KEYWORD

        parseCondition();

        PsiBuilder.Marker thenBranch = mark();
        if (!at(ELSE_KEYWORD) && !at(SEMICOLON)) {
            parseControlStructureBody();
        }
        if (at(SEMICOLON) && lookahead(1) == ELSE_KEYWORD) {
            advance(); // SEMICOLON
        }
        thenBranch.done(THEN);

        // lookahead for arrow is needed to prevent capturing of whenEntry like "else -> "
        if (at(ELSE_KEYWORD) && lookahead(1) != ARROW) {
            advance(); // ELSE_KEYWORD

            PsiBuilder.Marker elseBranch = mark();
            if (!at(SEMICOLON)) {
                parseControlStructureBody();
            }
            elseBranch.done(ELSE);
        }

        marker.done(IF);
    }

    /*
     * "(" element ")"
     */
    private void parseCondition() {
        myBuilder.disableNewlines();

        if (expect(LPAR, "Expecting a condition in parentheses '(...)'", EXPRESSION_FIRST)) {
            PsiBuilder.Marker condition = mark();
            parseExpression();
            condition.done(CONDITION);
            expect(RPAR, "Expecting ')");
        }

        myBuilder.restoreNewlinesState();
    }

    /*
     * : "continue" getEntryPoint?
     * : "break" getEntryPoint?
     */
    private void parseJump(IElementType type) {
        assert _at(BREAK_KEYWORD) || _at(CONTINUE_KEYWORD);

        PsiBuilder.Marker marker = mark();

        advance(); // BREAK_KEYWORD or CONTINUE_KEYWORD

        parseLabelReferenceWithNoWhitespace();

        marker.done(type);
    }

    /*
     * "return" getEntryPoint? element?
     */
    private void parseReturn() {
        assert _at(RETURN_KEYWORD);

        PsiBuilder.Marker returnExpression = mark();

        advance(); // RETURN_KEYWORD

        parseLabelReferenceWithNoWhitespace();

        if (atSet(EXPRESSION_FIRST) && !at(EOL_OR_SEMICOLON)) parseExpression();

        returnExpression.done(RETURN);
    }

    /*
     * labelReference?
     */
    private void parseLabelReferenceWithNoWhitespace() {
        if (at(AT) && !myBuilder.newlineBeforeCurrentToken()) {
            if (WHITE_SPACE_OR_COMMENT_BIT_SET.contains(myBuilder.rawLookup(-1))) {
                error("There should be no space or comments before '@' in label reference");
            }
            parseLabelReference();
        }
    }

    /*
     * IDENTIFIER "@"
     */
    void parseLabelDefinition() {
        assert isAtLabelDefinitionOrMissingIdentifier() : "Callers must check that current token is IDENTIFIER followed with '@'";

        PsiBuilder.Marker labelWrap = mark();
        PsiBuilder.Marker mark = mark();

        if (at(AT)) {
            errorAndAdvance("Expecting identifier before '@' in label definition");
            labelWrap.drop();
            mark.drop();
            return;
        }

        advance(); // IDENTIFIER
        advance(); // AT

        mark.done(LABEL);

        labelWrap.done(LABEL_QUALIFIER);
    }

    /*
     * "@" IDENTIFIER
     */
    private void parseLabelReference() {
        assert _at(AT);

        PsiBuilder.Marker labelWrap = mark();

        PsiBuilder.Marker mark = mark();

        if (myBuilder.rawLookup(1) != IDENTIFIER) {
            errorAndAdvance("Label must be named"); // AT
            labelWrap.drop();
            mark.drop();
            return;
        }

        advance(); // AT
        advance(); // IDENTIFIER

        mark.done(LABEL);

        labelWrap.done(LABEL_QUALIFIER);
    }

    /*
     * : "throw" element
     */
    private void parseThrow() {
        assert _at(THROW_KEYWORD);

        PsiBuilder.Marker marker = mark();

        advance(); // THROW_KEYWORD

        parseExpression();

        marker.done(THROW);
    }

    /*
     * "(" expression ")"
     */
    private void parseParenthesizedExpression() {
        assert _at(LPAR);

        PsiBuilder.Marker mark = mark();

        myBuilder.disableNewlines();
        advance(); // LPAR
        if (at(RPAR)) {
            error("Expecting an expression");
        }
        else {
            parseExpression();
        }

        expect(RPAR, "Expecting ')'");
        myBuilder.restoreNewlinesState();

        mark.done(PARENTHESIZED);
    }

    /*
     * "this" label?
     */
    private void parseThisExpression() {
        assert _at(THIS_KEYWORD);
        PsiBuilder.Marker mark = mark();

        PsiBuilder.Marker thisReference = mark();
        advance(); // THIS_KEYWORD
        thisReference.done(REFERENCE_EXPRESSION);

        parseLabelReferenceWithNoWhitespace();

        mark.done(THIS_EXPRESSION);
    }

    /*
     * "this" ("<" type ">")? label?
     */
    private void parseSuperExpression() {
        assert _at(SUPER_KEYWORD);
        PsiBuilder.Marker mark = mark();

        PsiBuilder.Marker superReference = mark();
        advance(); // SUPER_KEYWORD
        superReference.done(REFERENCE_EXPRESSION);

        if (at(LT)) {
            // This may be "super < foo" or "super<foo>", thus the backtracking
            PsiBuilder.Marker supertype = mark();

            myBuilder.disableNewlines();
            advance(); // LT

            myKotlinParsing.parseTypeRef();

            if (at(GT)) {
                advance(); // GT
                supertype.drop();
            }
            else {
                supertype.rollbackTo();
            }
            myBuilder.restoreNewlinesState();
        }
        parseLabelReferenceWithNoWhitespace();

        mark.done(SUPER_EXPRESSION);
    }

    /*
     * valueArguments
     *   : "(" (SimpleName "=")? "*"? element{","} ")"
     *   ;
     */
    public void parseValueArgumentList() {
        PsiBuilder.Marker list = mark();

        myBuilder.disableNewlines();

        if (expect(LPAR, "Expecting an argument list", EXPRESSION_FOLLOW)) {
            if (!at(RPAR)) {
                while (true) {
                    parseValueArgument();
                    if (at(COLON) && lookahead(1) == IDENTIFIER) {
                        errorAndAdvance("Unexpected type specification", 2);
                    }
                    if (!at(COMMA)) {
                        if (atSet(EXPRESSION_FIRST)) {
                            error("Expecting ','");
                            continue;
                        }
                        else {
                            break;
                        }
                    }
                    advance(); // COMMA
                    if (at(RPAR)) {
                        break;
                    }
                }
            }

            expect(RPAR, "Expecting ')'", EXPRESSION_FOLLOW);
        }

        myBuilder.restoreNewlinesState();

        list.done(VALUE_ARGUMENT_LIST);
    }

    /*
     * (SimpleName "=")? "*"? element
     */
    private void parseValueArgument() {
        PsiBuilder.Marker argument = mark();
        if (at(IDENTIFIER) && lookahead(1) == EQ) {
            PsiBuilder.Marker argName = mark();
            PsiBuilder.Marker reference = mark();
            advance(); // IDENTIFIER
            reference.done(REFERENCE_EXPRESSION);
            argName.done(VALUE_ARGUMENT_NAME);
            advance(); // EQ
        }
        if (at(MUL)) {
            advance(); // MUL
        }
        if (at(RPAR) || at(COMMA)) {
            PsiBuilder.Marker empty = mark();
            empty.done(EMPTY_VALUE_ARGUMENT);
        } else {
            parseExpression();
        }
        argument.done(VALUE_ARGUMENT);
    }

    /*
     * "object" (":" delegationSpecifier{","})? classBody // Cannot make class body optional: foo(object : F, A)
     */
    public void parseObjectLiteral() {
        PsiBuilder.Marker literal = mark();
        PsiBuilder.Marker declaration = mark();
        myKotlinParsing.parseObject(NameParsingMode.PROHIBITED, false); // Body is not optional because of foo(object : A, B)
        declaration.done(OBJECT_DECLARATION);
        literal.done(OBJECT_LITERAL);
    }

    private void parseOneTokenExpression(IElementType type) {
        PsiBuilder.Marker mark = mark();
        advance();
        mark.done(type);
    }

    @Override
    protected KotlinParsing create(SemanticWhitespaceAwarePsiBuilder builder) {
        return myKotlinParsing.create(builder);
    }

    private boolean interruptedWithNewLine() {
        return !ALLOW_NEWLINE_OPERATIONS.contains(tt()) && myBuilder.newlineBeforeCurrentToken();
    }
}
