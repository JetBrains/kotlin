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

package org.jetbrains.kotlin.parsing;

import com.google.common.collect.ImmutableMap;
import com.intellij.lang.PsiBuilder;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeType;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.lexer.KtToken;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.parsing.KotlinParsing.NameParsingMode;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.jetbrains.kotlin.KtNodeTypes.*;
import static org.jetbrains.kotlin.lexer.KtTokens.*;
import static org.jetbrains.kotlin.parsing.KotlinParsing.AnnotationParsingMode.DEFAULT;

public class KotlinExpressionParsing extends AbstractKotlinParsing {
    private static final TokenSet WHEN_CONDITION_RECOVERY_SET = TokenSet.create(RBRACE, IN_KEYWORD, NOT_IN, IS_KEYWORD, NOT_IS, ELSE_KEYWORD);
    private static final TokenSet WHEN_CONDITION_RECOVERY_SET_WITH_ARROW = TokenSet.create(RBRACE, IN_KEYWORD, NOT_IN, IS_KEYWORD, NOT_IS, ELSE_KEYWORD, ARROW, DOT);


    private static final ImmutableMap<String, KtToken> KEYWORD_TEXTS = tokenSetToMap(KEYWORDS);

    private static final IElementType[] LOCAL_DECLARATION_FIRST =
            new IElementType[] {CLASS_KEYWORD, INTERFACE_KEYWORD, FUN_KEYWORD, VAL_KEYWORD, VAR_KEYWORD, TYPE_ALIAS_KEYWORD};
    private static final TokenSet TOKEN_SET_TO_FOLLOW_AFTER_DESTRUCTURING_DECLARATION_IN_LAMBDA = TokenSet.create(ARROW, COMMA, COLON);

    private static ImmutableMap<String, KtToken> tokenSetToMap(TokenSet tokens) {
        ImmutableMap.Builder<String, KtToken> builder = ImmutableMap.builder();
        for (IElementType token : tokens.getTypes()) {
            builder.put(token.toString(), (KtToken) token);
        }
        return builder.build();
    }

    private static final TokenSet TYPE_ARGUMENT_LIST_STOPPERS = TokenSet.create(
            INTEGER_LITERAL, FLOAT_LITERAL, CHARACTER_LITERAL, OPEN_QUOTE,
            PACKAGE_KEYWORD, AS_KEYWORD, TYPE_ALIAS_KEYWORD, INTERFACE_KEYWORD, CLASS_KEYWORD, THIS_KEYWORD, VAL_KEYWORD, VAR_KEYWORD,
            FUN_KEYWORD, FOR_KEYWORD, NULL_KEYWORD,
            TRUE_KEYWORD, FALSE_KEYWORD, IS_KEYWORD, THROW_KEYWORD, RETURN_KEYWORD, BREAK_KEYWORD,
            CONTINUE_KEYWORD, OBJECT_KEYWORD, IF_KEYWORD, TRY_KEYWORD, ELSE_KEYWORD, WHILE_KEYWORD, DO_KEYWORD,
            WHEN_KEYWORD, RBRACKET, RBRACE, RPAR, PLUSPLUS, MINUSMINUS, EXCLEXCL,
            //            MUL,
            PLUS, MINUS, EXCL, DIV, PERC, LTEQ,
            // TODO GTEQ,   foo<bar, baz>=x
            EQEQEQ, EXCLEQEQEQ, EQEQ, EXCLEQ, ANDAND, OROR, SAFE_ACCESS, ELVIS,
            SEMICOLON, RANGE, EQ, MULTEQ, DIVEQ, PERCEQ, PLUSEQ, MINUSEQ, NOT_IN, NOT_IS,
            COLONCOLON,
            COLON
    );

    /*package*/ static final TokenSet EXPRESSION_FIRST = TokenSet.create(
            // Prefix
            MINUS, PLUS, MINUSMINUS, PLUSPLUS,
            EXCL, EXCLEXCL, // Joining complex tokens makes it necessary to put EXCLEXCL here
            // Atomic

            COLONCOLON, // callable reference

            LPAR, // parenthesized

            // literal constant
            TRUE_KEYWORD, FALSE_KEYWORD,
            OPEN_QUOTE,
            INTEGER_LITERAL, CHARACTER_LITERAL, FLOAT_LITERAL,
            NULL_KEYWORD,

            LBRACE, // functionLiteral
            FUN_KEYWORD, // expression function

            THIS_KEYWORD, // this
            SUPER_KEYWORD, // super

            IF_KEYWORD, // if
            WHEN_KEYWORD, // when
            TRY_KEYWORD, // try
            OBJECT_KEYWORD, // object

            // jump
            THROW_KEYWORD,
            RETURN_KEYWORD,
            CONTINUE_KEYWORD,
            BREAK_KEYWORD,

            // loop
            FOR_KEYWORD,
            WHILE_KEYWORD,
            DO_KEYWORD,

            IDENTIFIER, // SimpleName

            AT // Just for better recovery and maybe for annotations
    );

    private static final TokenSet STATEMENT_FIRST = TokenSet.orSet(
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

    /*package*/ static final TokenSet EXPRESSION_FOLLOW = TokenSet.create(
            EOL_OR_SEMICOLON, ARROW, COMMA, RBRACE, RPAR, RBRACKET
    );

    @SuppressWarnings({"UnusedDeclaration"})
    public enum Precedence {
        POSTFIX(PLUSPLUS, MINUSMINUS, EXCLEXCL,
                DOT, SAFE_ACCESS), // typeArguments? valueArguments : typeArguments : arrayAccess

        PREFIX(MINUS, PLUS, MINUSMINUS, PLUSPLUS, EXCL) { // annotations

            @Override
            public void parseHigherPrecedence(KotlinExpressionParsing parser) {
                throw new IllegalStateException("Don't call this method");
            }
        },

        AS(AS_KEYWORD, AS_SAFE) {
            @Override
            public KtNodeType parseRightHandSide(IElementType operation, KotlinExpressionParsing parser) {
                parser.myKotlinParsing.parseTypeRef();
                return BINARY_WITH_TYPE;
            }

            @Override
            public void parseHigherPrecedence(KotlinExpressionParsing parser) {
                parser.parsePrefixExpression();
            }
        },

        MULTIPLICATIVE(MUL, DIV, PERC),
        ADDITIVE(PLUS, MINUS),
        RANGE(KtTokens.RANGE),
        SIMPLE_NAME(IDENTIFIER),
        ELVIS(KtTokens.ELVIS),
        IN_OR_IS(IN_KEYWORD, NOT_IN, IS_KEYWORD, NOT_IS) {
            @Override
            public KtNodeType parseRightHandSide(IElementType operation, KotlinExpressionParsing parser) {
                if (operation == IS_KEYWORD || operation == NOT_IS) {
                    parser.myKotlinParsing.parseTypeRef();
                    return IS_EXPRESSION;
                }

                return super.parseRightHandSide(operation, parser);
            }
        },
        COMPARISON(LT, GT, LTEQ, GTEQ),
        EQUALITY(EQEQ, EXCLEQ, EQEQEQ, EXCLEQEQEQ),
        CONJUNCTION(ANDAND),
        DISJUNCTION(OROR),
        //        ARROW(KtTokens.ARROW),
        ASSIGNMENT(EQ, PLUSEQ, MINUSEQ, MULTEQ, DIVEQ, PERCEQ),
        ;

        static {
            Precedence[] values = Precedence.values();
            for (Precedence precedence : values) {
                int ordinal = precedence.ordinal();
                precedence.higher = ordinal > 0 ? values[ordinal - 1] : null;
            }
        }

        private Precedence higher;
        private final TokenSet operations;

        Precedence(IElementType... operations) {
            this.operations = TokenSet.create(operations);
        }

        public void parseHigherPrecedence(KotlinExpressionParsing parser) {
            assert higher != null;
            parser.parseBinaryExpression(higher);
        }

        /**
         *
         * @param operation the operation sign (e.g. PLUS or IS)
         * @param parser the parser object
         * @return node type of the result
         */
        public KtNodeType parseRightHandSide(IElementType operation, KotlinExpressionParsing parser) {
            parseHigherPrecedence(parser);
            return BINARY_EXPRESSION;
        }

        @NotNull
        public final TokenSet getOperations() {
            return operations;
        }
    }

    public static final TokenSet ALLOW_NEWLINE_OPERATIONS = TokenSet.create(
            DOT, SAFE_ACCESS,
            COLON, AS_KEYWORD, AS_SAFE,
            ELVIS,
            // Can't allow `is` and `!is` because of when entry conditions: IS_KEYWORD, NOT_IS,
            ANDAND,
            OROR
    );

    public static final TokenSet ALL_OPERATIONS;

    static {
        Set<IElementType> operations = new HashSet<IElementType>();
        Precedence[] values = Precedence.values();
        for (Precedence precedence : values) {
            operations.addAll(Arrays.asList(precedence.getOperations().getTypes()));
        }
        ALL_OPERATIONS = TokenSet.create(operations.toArray(new IElementType[operations.size()]));
    }

    static {
        IElementType[] operations = OPERATIONS.getTypes();
        Set<IElementType> opSet = new HashSet<IElementType>(Arrays.asList(operations));
        IElementType[] usedOperations = ALL_OPERATIONS.getTypes();
        Set<IElementType> usedSet = new HashSet<IElementType>(Arrays.asList(usedOperations));

        if (opSet.size() > usedSet.size()) {
            opSet.removeAll(usedSet);
            assert false : opSet;
        }
        assert usedSet.size() == opSet.size() : "Either some ops are unused, or something a non-op is used";

        usedSet.removeAll(opSet);

        assert usedSet.isEmpty() : usedSet.toString();
    }


    private final KotlinParsing myKotlinParsing;

    public KotlinExpressionParsing(SemanticWhitespaceAwarePsiBuilder builder, KotlinParsing kotlinParsing) {
        super(builder);
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
        parseBinaryExpression(Precedence.ASSIGNMENT);
    }

    /*
     * element (operation element)*
     *
     * see the precedence table
     */
    private void parseBinaryExpression(Precedence precedence) {
        PsiBuilder.Marker expression = mark();

        precedence.parseHigherPrecedence(this);

        while (!interruptedWithNewLine() && atSet(precedence.getOperations())) {
            IElementType operation = tt();

            parseOperationReference();

            KtNodeType resultType = precedence.parseRightHandSide(operation, this);
            expression.done(resultType);
            expression = expression.precede();
        }

        expression.drop();
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
            if (!parseLocalDeclaration(/* rollbackIfDefinitelyNotExpression = */ false)) {
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
            else if (atSet(Precedence.PREFIX.getOperations())) {
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
                }

                parseSelectorCallExpression();

                if (firstExpressionParsed) {
                    expression.done(expressionType);
                }
                else {
                    firstExpressionParsed = true;
                    continue;
                }
            }
            else if (atSet(Precedence.POSTFIX.getOperations())) {
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

    private boolean isAtLabelDefinitionOrMissingIdentifier() {
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
     *   ;
     */
    private boolean parseAtomicExpression() {
        boolean ok = true;

        if (at(LPAR)) {
            parseParenthesizedExpression();
        }
        else if (at(THIS_KEYWORD)) {
            parseThisExpression();
        }
        else if (at(SUPER_KEYWORD)) {
            parseSuperExpression();
        }
        else if (at(OBJECT_KEYWORD)) {
            parseObjectLiteral();
        }
        else if (at(THROW_KEYWORD)) {
            parseThrow();
        }
        else if (at(RETURN_KEYWORD)) {
            parseReturn();
        }
        else if (at(CONTINUE_KEYWORD)) {
            parseJump(CONTINUE);
        }
        else if (at(BREAK_KEYWORD)) {
            parseJump(BREAK);
        }
        else if (at(IF_KEYWORD)) {
            parseIf();
        }
        else if (at(WHEN_KEYWORD)) {
            parseWhen();
        }
        else if (at(TRY_KEYWORD)) {
            parseTry();
        }
        else if (at(FOR_KEYWORD)) {
            parseFor();
        }
        else if (at(WHILE_KEYWORD)) {
            parseWhile();
        }
        else if (at(DO_KEYWORD)) {
            parseDoWhile();
        }
        else if (atSet(LOCAL_DECLARATION_FIRST) &&
                    parseLocalDeclaration(/* rollbackIfDefinitelyNotExpression = */ myBuilder.newlineBeforeCurrentToken())) {
            // declaration was parsed, do nothing
        }
        else if (at(IDENTIFIER)) {
            parseSimpleNameExpression();
        }
        else if (at(LBRACE)) {
            parseFunctionLiteral();
        }
        else if (at(OPEN_QUOTE)) {
            parseStringTemplate();
        }
        else if (!parseLiteralConstant()) {
            ok = false;
            // TODO: better recovery if FIRST(element) did not match
            errorWithRecovery("Expecting an element", EXPRESSION_FOLLOW);
        }

        return ok;
    }

    /*
     * stringTemplate
     *   : OPEN_QUOTE stringTemplateElement* CLOSING_QUOTE
     *   ;
     */
    private void parseStringTemplate() {
        assert _at(OPEN_QUOTE);

        PsiBuilder.Marker template = mark();

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

            parseExpression();

            expect(LONG_TEMPLATE_ENTRY_END, "Expecting '}'", TokenSet.create(CLOSING_QUOTE, DANGLING_NEWLINE, REGULAR_STRING_PART, ESCAPE_SEQUENCE, SHORT_TEMPLATE_ENTRY_START));
            longTemplateEntry.done(LONG_STRING_TEMPLATE_ENTRY);
        }
        else {
            errorAndAdvance("Unexpected token in a string template");
        }
    }

    /*
     * literalConstant
     *   : "true" | "false"
     *   : stringTemplate
     *   : NoEscapeString
     *   : IntegerLiteral
     *   : LongLiteral
     *   : CharacterLiteral
     *   : FloatLiteral
     *   : "null"
     *   ;
     */
    private boolean parseLiteralConstant() {
        if (at(TRUE_KEYWORD) || at(FALSE_KEYWORD)) {
            parseOneTokenExpression(BOOLEAN_CONSTANT);
        }
        else if (at(INTEGER_LITERAL)) {
            parseOneTokenExpression(INTEGER_CONSTANT);
        }
        else if (at(CHARACTER_LITERAL)) {
            parseOneTokenExpression(CHARACTER_CONSTANT);
        }
        else if (at(FLOAT_LITERAL)) {
            parseOneTokenExpression(FLOAT_CONSTANT);
        }
        else if (at(NULL_KEYWORD)) {
            parseOneTokenExpression(NULL);
        }
        else {
            return false;
        }
        return true;
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

            PsiBuilder.Marker property = mark();
            myKotlinParsing.parseModifierList(DEFAULT, TokenSet.create(EQ, RPAR));
            if (at(VAL_KEYWORD) || at(VAR_KEYWORD)) {
                myKotlinParsing.parseProperty(true);
                property.done(PROPERTY);
            }
            else {
                property.rollbackTo();
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
     *   : whenCondition{","} "->" element SEMI
     *   : "else" "->" element SEMI
     *   ;
     */
    private void parseWhenEntry() {
        PsiBuilder.Marker entry = mark();

        if (at(ELSE_KEYWORD)) {
            advance(); // ELSE_KEYWORD

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
     * : whenCondition{","} "->" element SEMI
     */
    private void parseWhenEntryNotElse() {
        while (true) {
            while (at(COMMA)) errorAndAdvance("Expecting a when-condition");
            parseWhenCondition();
            if (!at(COMMA)) break;
            advance(); // COMMA
        }

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
        if (at(IN_KEYWORD) || at(NOT_IN)) {
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
        }
        else if (at(IS_KEYWORD) || at(NOT_IS)) {
            advance(); // IS_KEYWORD or NOT_IS

            if (atSet(WHEN_CONDITION_RECOVERY_SET_WITH_ARROW)) {
                error("Expecting a type");
            }
            else {
                myKotlinParsing.parseTypeRef();
            }
            condition.done(WHEN_CONDITION_IS_PATTERN);
        }
        else {
            if (atSet(WHEN_CONDITION_RECOVERY_SET_WITH_ARROW)) {
                error("Expecting an expression, is-condition or in-condition");
            }
            else {
                parseExpression();
            }
            condition.done(WHEN_CONDITION_EXPRESSION);
        }
        myBuilder.restoreNewlinesState();
    }

    /*
     * arrayAccess
     *   : "[" element{","} "]"
     *   ;
     */
    private void parseArrayAccess() {
        assert _at(LBRACKET);

        PsiBuilder.Marker indices = mark();

        myBuilder.disableNewlines();
        advance(); // LBRACKET

        while (true) {
            if (at(COMMA)) errorAndAdvance("Expecting an index element");
            if (at(RBRACKET)) {
                error("Expecting an index element");
                break;
            }
            parseExpression();
            if (!at(COMMA)) break;
            advance(); // COMMA
        }

        expect(RBRACKET, "Expecting ']'");
        myBuilder.restoreNewlinesState();

        indices.done(INDICES);
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
    private boolean parseLocalDeclaration(boolean rollbackIfDefinitelyNotExpression) {
        PsiBuilder.Marker decl = mark();
        KotlinParsing.ModifierDetector detector = new KotlinParsing.ModifierDetector();
        myKotlinParsing.parseModifierList(detector, DEFAULT, TokenSet.EMPTY);

        IElementType declType = parseLocalDeclarationRest(detector.isEnumDetected(), rollbackIfDefinitelyNotExpression);

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
     */
    public void parseFunctionLiteral(boolean preferBlock, boolean collapse) {
        assert _at(LBRACE);

        PsiBuilder.Marker literalExpression = mark();

        PsiBuilder.Marker literal = mark();

        myBuilder.enableNewlines();
        advance(); // LBRACE

        boolean paramsFound = false;

        if (at(ARROW)) {
            //   { -> ...}
            mark().done(VALUE_PARAMETER_LIST);
            advance(); // ARROW
            paramsFound = true;
        }
        else if (at(IDENTIFIER) || at(COLON) || at(LPAR)) {
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

        if (collapse) {
            advanceLambdaBlock();
            literal.done(FUNCTION_LITERAL);
            literalExpression.collapse(LAMBDA_EXPRESSION);
        }
        else {
            PsiBuilder.Marker body = mark();
            parseStatements();
            body.done(BLOCK);

            expect(RBRACE, "Expecting '}'");
            literal.done(FUNCTION_LITERAL);
            literalExpression.done(LAMBDA_EXPRESSION);
        }

        myBuilder.restoreNewlinesState();
    }

    private void advanceLambdaBlock() {
        int braceCount = 1;
        while (!eof()) {
            if (_at(LBRACE)) {
                braceCount++;
            }
            else if (_at(RBRACE)) {
                braceCount--;
            }

            advance();

            if (braceCount == 0) {
                break;
            }
        }
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
            PsiBuilder.Marker parameter = mark();

            if (at(COLON)) {
                error("Expecting parameter name");
            }
            else if (at(LPAR)) {
                PsiBuilder.Marker destructuringDeclaration = mark();
                myKotlinParsing.parseMultiDeclarationName(TOKEN_SET_TO_FOLLOW_AFTER_DESTRUCTURING_DECLARATION_IN_LAMBDA);
                destructuringDeclaration.done(DESTRUCTURING_DECLARATION);
            }
            else {
                expect(IDENTIFIER, "Expecting parameter name", TokenSet.create(ARROW));
            }

            if (at(COLON)) {
                advance(); // COLON
                myKotlinParsing.parseTypeRef(TokenSet.create(ARROW, COMMA));
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
            else if (!myBuilder.newlineBeforeCurrentToken()) {
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
     *  : annotations expression
     *  ;
     */
    private void parseStatement(boolean isScriptTopLevel) {
        if (!parseLocalDeclaration(/* rollbackIfDefinitelyNotExpression = */false)) {
            if (!atSet(EXPRESSION_FIRST)) {
                errorAndAdvance("Expecting a statement");
            }
            else if (isScriptTopLevel){
                PsiBuilder.Marker scriptInitializer = mark();
                parseStatementLevelExpression();
                scriptInitializer.done(SCRIPT_INITIALIZER);
            }
            else {
                parseStatementLevelExpression();
            }
        }
    }

    private void parseStatementLevelExpression() {
        if (at(AT)) {
            PsiBuilder.Marker expression = mark();
            myKotlinParsing.parseAnnotations(DEFAULT);
            parseStatementLevelExpression();
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
    private IElementType parseLocalDeclarationRest(boolean isEnum, boolean failIfDefinitelyNotExpression) {
        IElementType keywordToken = tt();
        IElementType declType = null;

        if (failIfDefinitelyNotExpression) {
            if (keywordToken != FUN_KEYWORD) return null;

            return myKotlinParsing.parseFunction(/* failIfIdentifierExists = */ true);
        }

        if (keywordToken == CLASS_KEYWORD ||  keywordToken == INTERFACE_KEYWORD) {
            declType = myKotlinParsing.parseClass(isEnum);
        }
        else if (keywordToken == FUN_KEYWORD) {
            declType = myKotlinParsing.parseFunction();
        }
        else if (keywordToken == VAL_KEYWORD || keywordToken == VAR_KEYWORD) {
            declType = myKotlinParsing.parseProperty(true);
        }
        else if (keywordToken == TYPE_ALIAS_KEYWORD) {
            declType = myKotlinParsing.parseTypeAlias();
        }
        else if (keywordToken == OBJECT_KEYWORD) {
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

            myKotlinParsing.parseObject(NameParsingMode.REQUIRED, true);
            declType = OBJECT_DECLARATION;
        }
        return declType;
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
                    myKotlinParsing.parseModifierList(DEFAULT, TokenSet.create(IN_KEYWORD, RPAR, COLON));
                }

                if (at(VAL_KEYWORD) || at(VAR_KEYWORD)) advance(); // VAL_KEYWORD or VAR_KEYWORD

                if (at(LPAR)) {
                    PsiBuilder.Marker destructuringDeclaration = mark();
                    myKotlinParsing.parseMultiDeclarationName(TokenSet.create(IN_KEYWORD, LBRACE));
                    destructuringDeclaration.done(DESTRUCTURING_DECLARATION);
                }
                else {
                    expect(IDENTIFIER, "Expecting a variable name", TokenSet.create(COLON, IN_KEYWORD));

                    if (at(COLON)) {
                        advance(); // COLON
                        myKotlinParsing.parseTypeRef(TokenSet.create(IN_KEYWORD));
                    }
                }
                parameter.done(VALUE_PARAMETER);

                if (expect(IN_KEYWORD, "Expecting 'in'", TokenSet.create(LPAR, LBRACE, RPAR))) {
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
            parseExpression();
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

            TokenSet recoverySet = TokenSet.create(LBRACE, RBRACE, FINALLY_KEYWORD, CATCH_KEYWORD);
            if (atSet(recoverySet)) {
                error("Expecting exception variable declaration");
            }
            else {
                PsiBuilder.Marker parameters = mark();
                expect(LPAR, "Expecting '('", recoverySet);
                if (!atSet(recoverySet)) {
                    myKotlinParsing.parseValueParameter(/*typeRequired = */ true);
                    expect(RPAR, "Expecting ')'", recoverySet);
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
    private void parseJump(KtNodeType type) {
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
    private void parseLabelDefinition() {
        PsiBuilder.Marker labelWrap = mark();
        PsiBuilder.Marker mark = mark();

        assert _at(IDENTIFIER) && myBuilder.rawLookup(1) == AT : "Callers must check that current token is IDENTIFIER followed with '@'";

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
                    while (at(COMMA)) errorAndAdvance("Expecting an argument");
                    parseValueArgument();
                    if (at(COLON) && lookahead(1) == IDENTIFIER) {
                        errorAndAdvance("Unexpected type specification", 2);
                    }
                    if (!at(COMMA)) break;
                    advance(); // COMMA
                    if (at(RPAR)) {
                        error("Expecting an argument");
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
        parseExpression();
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

    private void parseOneTokenExpression(KtNodeType type) {
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
