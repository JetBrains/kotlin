/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.parsing;

import com.google.common.collect.ImmutableMap;
import com.intellij.lang.PsiBuilder;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.jet.JetNodeType;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.*;

import static org.jetbrains.jet.JetNodeTypes.*;
import static org.jetbrains.jet.lexer.JetTokens.*;

/**
 * @author abreslav
 */
public class JetExpressionParsing extends AbstractJetParsing {
    private static final TokenSet WHEN_CONDITION_RECOVERY_SET = TokenSet.create(RBRACE, IN_KEYWORD, NOT_IN, IS_KEYWORD, NOT_IS, ELSE_KEYWORD);
    private static final TokenSet WHEN_CONDITION_RECOVERY_SET_WITH_ARROW = TokenSet.create(RBRACE, IN_KEYWORD, NOT_IN, IS_KEYWORD, NOT_IS, ELSE_KEYWORD, ARROW, DOT);


    private static final ImmutableMap<String, JetToken> KEYWORD_TEXTS = tokenSetToMap(KEYWORDS);

    private static ImmutableMap<String, JetToken> tokenSetToMap(TokenSet tokens) {
        ImmutableMap.Builder<String, JetToken> builder = ImmutableMap.builder();
        for (IElementType token : tokens.getTypes()) {
            builder.put(token.toString(), (JetToken) token);
        }
        return builder.build();
    }

    private static final TokenSet TYPE_ARGUMENT_LIST_STOPPERS = TokenSet.create(
            INTEGER_LITERAL, FLOAT_LITERAL, CHARACTER_LITERAL, OPEN_QUOTE,
            PACKAGE_KEYWORD, AS_KEYWORD, TYPE_KEYWORD, TRAIT_KEYWORD, CLASS_KEYWORD, THIS_KEYWORD, VAL_KEYWORD, VAR_KEYWORD,
            FUN_KEYWORD, FOR_KEYWORD, NULL_KEYWORD,
            TRUE_KEYWORD, FALSE_KEYWORD, IS_KEYWORD, THROW_KEYWORD, RETURN_KEYWORD, BREAK_KEYWORD,
            CONTINUE_KEYWORD, OBJECT_KEYWORD, IF_KEYWORD, TRY_KEYWORD, ELSE_KEYWORD, WHILE_KEYWORD, DO_KEYWORD,
            WHEN_KEYWORD, RBRACKET, RBRACE, RPAR, PLUSPLUS, MINUSMINUS, EXCLEXCL,
//            MUL,
            PLUS, MINUS, EXCL, DIV, PERC, LTEQ,
            // TODO GTEQ,   foo<bar, baz>=x
            EQEQEQ, EXCLEQEQEQ, EQEQ, EXCLEQ, ANDAND, OROR, SAFE_ACCESS, ELVIS,
            SEMICOLON, RANGE, EQ, MULTEQ, DIVEQ, PERCEQ, PLUSEQ, MINUSEQ, NOT_IN, NOT_IS, //HASH,
            COLON
    );

    /*package*/ static final TokenSet EXPRESSION_FIRST = TokenSet.create(
            // Prefix
            MINUS, PLUS, MINUSMINUS, PLUSPLUS,
            EXCL, EXCLEXCL, // Joining complex tokens makes it necessary to put EXCLEXCL here
            LBRACKET, LABEL_IDENTIFIER, AT, ATAT,
            // Atomic

            LPAR, // parenthesized
            HASH, // Tuple

            // literal constant
            TRUE_KEYWORD, FALSE_KEYWORD,
            OPEN_QUOTE,
            INTEGER_LITERAL, CHARACTER_LITERAL, FLOAT_LITERAL,
            NULL_KEYWORD,

            LBRACE, // functionLiteral

            LPAR, // tuple

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
            FIELD_IDENTIFIER, // Field reference

            PACKAGE_KEYWORD, // for absolute qualified names
            IDE_TEMPLATE_START
    );

    private static final TokenSet STATEMENT_FIRST = TokenSet.orSet(
        EXPRESSION_FIRST,
        TokenSet.create(
            // declaration
            LBRACKET, // attribute
            FUN_KEYWORD,
            VAL_KEYWORD, VAR_KEYWORD,
            TRAIT_KEYWORD,
            CLASS_KEYWORD,
            TYPE_KEYWORD
        ),
        MODIFIER_KEYWORDS
    );

    /*package*/ static final TokenSet EXPRESSION_FOLLOW = TokenSet.create(
            SEMICOLON, ARROW, COMMA, RBRACE, RPAR, RBRACKET, IDE_TEMPLATE_END
    );

    @SuppressWarnings({"UnusedDeclaration"})
    private enum Precedence {
        POSTFIX(PLUSPLUS, MINUSMINUS, EXCLEXCL,
//                HASH,
                DOT, SAFE_ACCESS), // typeArguments? valueArguments : typeArguments : arrayAccess

        PREFIX(MINUS, PLUS, MINUSMINUS, PLUSPLUS, EXCL, LABEL_IDENTIFIER, AT, ATAT) { // attributes

            @Override
            public void parseHigherPrecedence(JetExpressionParsing parser) {
                throw new IllegalStateException("Don't call this method");
            }
        },

        COLON_AS(COLON, AS_KEYWORD, AS_SAFE) {
            @Override
            public JetNodeType parseRightHandSide(IElementType operation, JetExpressionParsing parser) {
                parser.myJetParsing.parseTypeRef();
                return BINARY_WITH_TYPE;
            }

            @Override
            public void parseHigherPrecedence(JetExpressionParsing parser) {
                parser.parsePrefixExpression();
            }
        },

        MULTIPLICATIVE(MUL, DIV, PERC),
        ADDITIVE(PLUS, MINUS),
        RANGE(JetTokens.RANGE),
        SIMPLE_NAME(IDENTIFIER),
        ELVIS(JetTokens.ELVIS),
        IN_OR_IS(IN_KEYWORD, NOT_IN, IS_KEYWORD, NOT_IS) {
            @Override
            public JetNodeType parseRightHandSide(IElementType operation, JetExpressionParsing parser) {
                if (operation == IS_KEYWORD || operation == NOT_IS) {
                    parser.parsePattern();

                    return BINARY_WITH_PATTERN;
                }

                return super.parseRightHandSide(operation, parser);
            }
        },
        COMPARISON(LT, GT, LTEQ, GTEQ),
        EQUALITY(EQEQ, EXCLEQ, EQEQEQ, EXCLEQEQEQ),
        CONJUNCTION(ANDAND),
        DISJUNCTION(OROR),
//        ARROW(JetTokens.ARROW),
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

        public void parseHigherPrecedence(JetExpressionParsing parser) {
            assert higher != null;
            parser.parseBinaryExpression(higher);
        }

        /**
         *
         * @param operation the operation sign (e.g. PLUS or IS)
         * @param parser the parser object
         * @return node type of the result
         */
        public JetNodeType parseRightHandSide(IElementType operation, JetExpressionParsing parser) {
            parseHigherPrecedence(parser);
            return BINARY_EXPRESSION;
        }

        public final TokenSet getOperations() {
            return operations;
        }
    }

    public static final TokenSet ALLOW_NEWLINE_OPERATIONS = TokenSet.create(DOT, SAFE_ACCESS);

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


    private final JetParsing myJetParsing;
    private TokenSet decomposerExpressionFollow = null;

    public JetExpressionParsing(SemanticWhitespaceAwarePsiBuilder builder, JetParsing jetParsing) {
        super(builder);
        myJetParsing = jetParsing;
    }

    private TokenSet getDecomposerExpressionFollow() {
        // TODO : memoize
        if (decomposerExpressionFollow == null) {
            List<IElementType> elvisFollow = new ArrayList<IElementType>();
            Precedence precedence = Precedence.ELVIS;
            while (precedence != null) {
                IElementType[] types = precedence.getOperations().getTypes();
                Collections.addAll(elvisFollow, types);
                precedence = precedence.higher;
            }
            decomposerExpressionFollow = TokenSet.orSet(EXPRESSION_FOLLOW, TokenSet.create(elvisFollow.toArray(new IElementType[elvisFollow.size()])));
        }
        return decomposerExpressionFollow;
    }

    /*
     * element
     *   : attributes element
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
//        System.out.println(precedence.name() + " at " + myBuilder.getTokenText());

        PsiBuilder.Marker expression = mark();

        precedence.parseHigherPrecedence(this);

        while (!interruptedWithNewLine() && atSet(precedence.getOperations())) {
            IElementType operation = tt();

            parseOperationReference();

            JetNodeType resultType = precedence.parseRightHandSide(operation, this);
            expression.done(resultType);
             expression = expression.precede();
        }

        expression.drop();
    }

    /*
     * operation? prefixExpression
     */
    private void parsePrefixExpression() {
//        System.out.println("pre at "  + myBuilder.getTokenText());

        if (at(LBRACKET)) {
            if (!parseLocalDeclaration()) {
                PsiBuilder.Marker expression = mark();
                myJetParsing.parseAnnotations(false);
                parsePrefixExpression();
                expression.done(ANNOTATED_EXPRESSION);
            }
            else {
                return;
            }
        }
        else {
            myBuilder.disableJoiningComplexTokens();
            if (atSet(Precedence.PREFIX.getOperations())) {
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
     * atomicExpression postfixUnaryOperation?
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
//        System.out.println("post at "  + myBuilder.getTokenText());

        PsiBuilder.Marker expression = mark();
        parseAtomicExpression();
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
            else if (at(DOT)) {
                advance(); // DOT

                parseCallExpression();

                expression.done(DOT_QUALIFIED_EXPRESSION);
            }
            else if (at(SAFE_ACCESS)) {
                advance(); // SAFE_ACCESS

                parseCallExpression();

                expression.done(SAFE_ACCESS_EXPRESSION);
            }
//            else if (at(HASH)) {
//                advance(); // HASH
//
//                expect(IDENTIFIER, "Expecting property or function name");
//
//                expression.done(HASH_QUALIFIED_EXPRESSION);
//            }
            else if (atSet(Precedence.POSTFIX.getOperations())) {
                parseOperationReference();
                expression.done(POSTFIX_EXPRESSION);
            }
            else {
                break;
            }
            expression = expression.precede();
        }
        expression.drop();
    }

    /*
     * callSuffix
     *   : typeArguments? valueArguments (getEntryPoint? functionLiteral*)
     *   : typeArguments (getEntryPoint? functionLiteral*)
     *   ;
     */
    private boolean parseCallSuffix() {
        if (parseCallWithClosure()) {
            parseCallWithClosure();
        }
        else if (at(LPAR)) {
            parseValueArgumentList();
            parseCallWithClosure();
        }
        else if (at(LT)) {
            PsiBuilder.Marker typeArgumentList = mark();
            if (myJetParsing.tryParseTypeArgumentList(TYPE_ARGUMENT_LIST_STOPPERS)) {
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
    private void parseCallExpression() {
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
     * element (getEntryPoint? functionLiteral)?
     */
    protected boolean parseCallWithClosure() {
        boolean success = false;
//        while (!myBuilder.newlineBeforeCurrentToken()
//                && (at(LBRACE)
        while ((at(LBRACE)
                    || atSet(LABELS) && lookahead(1) == LBRACE)) {
            if (!at(LBRACE)) {
                assert _atSet(LABELS);
                parsePrefixExpression();
            }
            else {
                parseFunctionLiteral();
            }
            success = true;
        }
        return success;
    }

    /*
     * atomicExpression
     *   : tupleLiteral // or parenthesized element
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
     *   : "package" // foo the root namespace
     *   ;
     */
    private void parseAtomicExpression() {
//        System.out.println("atom at "  + myBuilder.getTokenText());

        if (at(LPAR)) {
            parseParenthesizedExpression();
        }
        else if (at(IDE_TEMPLATE_START)) {
            myJetParsing.parseIdeTemplate();
        }
        else if (at(HASH)) {
            parseTupleExpression();
        }
        else if (at(PACKAGE_KEYWORD)) {
            parseOneTokenExpression(ROOT_NAMESPACE);
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
        else if (atSet(CLASS_KEYWORD, FUN_KEYWORD, VAL_KEYWORD,
                VAR_KEYWORD, TYPE_KEYWORD)) {
            parseLocalDeclaration();
        }
        else if (at(FIELD_IDENTIFIER)) {
            parseSimpleNameExpression();
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
            // TODO: better recovery if FIRST(element) did not match
            errorWithRecovery("Expecting an element", EXPRESSION_FOLLOW);
        }
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
                JetToken keyword = KEYWORD_TEXTS.get(myBuilder.getTokenText());
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
     *   : StringWithTemplates
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

            int valPos = matchTokenStreamPredicate(new FirstBefore(new At(VAL_KEYWORD), new AtSet(RPAR, LBRACE, RBRACE, SEMICOLON, EQ)));
            if (valPos >= 0) {
                PsiBuilder.Marker property = mark();
                myJetParsing.parseModifierList(MODIFIER_LIST, true);
                myJetParsing.parseProperty(true);
                property.done(PROPERTY);
            }
            else {
                parseExpression();
            }

            expect(RPAR, "Expecting ')'");
        }
        myBuilder.restoreNewlinesState();

        // Parse when block
        myBuilder.enableNewlines();
        expect(LBRACE, "Expecting '{'");

        while (!eof() && !at(RBRACE)) {
            parseWhenEntry();
        }

        expect(RBRACE, "Expecting '}'");
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
                errorUntil("Expecting '->'", TokenSet.create(ARROW,
                        RBRACE, EOL_OR_SEMICOLON));
            }

            if (at(ARROW)) {
                advance(); // ARROW

                if (atSet(WHEN_CONDITION_RECOVERY_SET)) {
                    error("Expecting an element");
                }
                else {
                    parseExpressionPreferringBlocks();
                }
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
        if (!myJetParsing.parseIdeTemplate()) {
            while (true) {
                while (at(COMMA)) errorAndAdvance("Expecting a when-condition");
                parseWhenCondition();
                if (!at(COMMA)) break;
                advance(); // COMMA
            }
        }
        expect(ARROW, "Expecting '->' or 'when'", WHEN_CONDITION_RECOVERY_SET);
        if (atSet(WHEN_CONDITION_RECOVERY_SET)) {
            error("Expecting an element");
        }
        else {
            parseExpressionPreferringBlocks();
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
                error("Expecting a type or a decomposer pattern");
            }
            else {
                parsePattern();
            }
            condition.done(WHEN_CONDITION_IS_PATTERN);
        }
        else {
            PsiBuilder.Marker expressionPattern = mark();
            if (atSet(WHEN_CONDITION_RECOVERY_SET_WITH_ARROW)) {
                error("Expecting an expression, is-condition or in-condition");
            }
            else {
                parseExpression();
            }
            expressionPattern.done(EXPRESSION_PATTERN);
            condition.done(WHEN_CONDITION_EXPRESSION);
        }
        myBuilder.restoreNewlinesState();
    }

    /*
     * pattern
     *   : attributes pattern
     *   : type // '[a] T' is a type-pattern 'T' with an attribute '[a]', not a type-pattern '[a] T'
     *          // this makes sense because is-check may be different for a type with attributes
     *   : tuplePattern
     *   : decomposerPattern
     *   : constantPattern
     *   : bindingPattern
     *   : "*" // wildcard pattern
     *   ;
     */
    private void parsePattern() {
        PsiBuilder.Marker pattern = mark();

        myJetParsing.parseAnnotations(false);

        if (at(PACKAGE_KEYWORD) || at(IDENTIFIER) || at(FUN_KEYWORD) || at(THIS_KEYWORD)) {
            PsiBuilder.Marker rollbackMarker = mark();
            parseBinaryExpression(Precedence.ELVIS);
            if (at(HASH)) {
                rollbackMarker.drop();
                PsiBuilder.Marker list = mark();
                parseTuplePattern(DECOMPOSER_ARGUMENT);
                list.done(DECOMPOSER_ARGUMENT_LIST);
                pattern.done(DECOMPOSER_PATTERN);
            }
            else {
                int expressionEndOffset = myBuilder.getCurrentOffset();
                rollbackMarker.rollbackTo();
                rollbackMarker = mark();

                myJetParsing.parseTypeRef();
//                if (at(AT)) {
//                    errorAndAdvance("'@' is allowed only after a decomposer element, not after a type");
//                }
                if (myBuilder.getCurrentOffset() < expressionEndOffset) {
                    rollbackMarker.rollbackTo();
                    parseBinaryExpression(Precedence.ELVIS);
                    pattern.done(DECOMPOSER_PATTERN);
                }
                else {
                    rollbackMarker.drop();
                    pattern.done(TYPE_PATTERN);
                }
            }
        }
        else if (at(HASH)) {
            parseTuplePattern(TUPLE_PATTERN_ENTRY);
            pattern.done(TUPLE_PATTERN);
        }
        else if (at(MUL)) {
            advance(); // MUL
            pattern.done(WILDCARD_PATTERN);
        }
        else if (at(VAL_KEYWORD)) {
            parseBindingPattern();
            pattern.done(BINDING_PATTERN);
        }
        else if (at(OPEN_QUOTE)) {
            parseStringTemplate();
            pattern.done(EXPRESSION_PATTERN);
        }
        else if (parseLiteralConstant()) {
            pattern.done(EXPRESSION_PATTERN);
        }
        else {
            errorUntil("Pattern expected", TokenSet.create(RBRACE, ARROW));
            pattern.drop();
        }
    }

    /*
     * tuplePattern
     *  : "#" "(" ((SimpleName "=")? pattern){","}? ")"
     *  ;
     */
    private void parseTuplePattern(JetNodeType entryType) {

        myBuilder.disableNewlines();
        expect(HASH, "Expecting a tuple pattern of the form '#(...)'", getDecomposerExpressionFollow());
        expect(LPAR, "Expecting a tuple pattern of the form '#(...)'", getDecomposerExpressionFollow());

        if (!at(RPAR)) {
            while (true) {
                while (at(COMMA)) errorAndAdvance("Expecting a pattern");
                if (at(RPAR)) {
                    error("Expecting a pattern");
                    break;
                }
                PsiBuilder.Marker entry = mark();
                if (at(IDENTIFIER) && lookahead(1) == EQ) {
                    advance(); // IDENTIFIER
                    advance(); // EQ
                }
                parsePattern();
                entry.done(entryType);
                if (!at(COMMA)) break;

                advance(); // COMMA
            }
        }

        expect(RPAR, "Expecting ')'");
        myBuilder.restoreNewlinesState();
    }

    /*
     * bindingPattern
     *   : "val" SimpleName binding?
     *   ;
     *
     * binding
     *   : "is" pattern
     *   : "!is" pattern
     *   : "in" element
     *   : "!in" element
     *   : ":" type
     *   ;
     */
    private void parseBindingPattern() {
        assert _at(VAL_KEYWORD);

        PsiBuilder.Marker declaration = mark();

        advance(); // VAL_KEYWORD

        expect(IDENTIFIER, "Expecting an identifier");

        if (at(COLON)) {
            advance(); // EQ

            myJetParsing.parseTypeRef();
            declaration.done(PROPERTY);
        }
        else {
            declaration.done(PROPERTY);
            PsiBuilder.Marker subCondition = mark();
            if (at(IS_KEYWORD) || at(NOT_IS)) {

                advance(); // IS_KEYWORD or NOT_IS

                parsePattern();
                subCondition.done(WHEN_CONDITION_IS_PATTERN);
            }
            else if (at(IN_KEYWORD) || at(NOT_IN)) {
                PsiBuilder.Marker mark = mark();
                advance(); // IN_KEYWORD ot NOT_IN
                mark.done(OPERATION_REFERENCE);

                parseExpression();
                subCondition.done(WHEN_CONDITION_IN_RANGE);
            }
            else {
                subCondition.drop();
            }
        }
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
        if (at(FIELD_IDENTIFIER)) {
            advance(); //
        }
        else {
            expect(IDENTIFIER, "Expecting an identifier");
        }
        simpleName.done(REFERENCE_EXPRESSION);
    }

    /*
     * modifiers declarationRest
     */
    private boolean parseLocalDeclaration() {
        PsiBuilder.Marker decl = mark();
        JetParsing.TokenDetector enumDetector = new JetParsing.TokenDetector(ENUM_KEYWORD);
        myJetParsing.parseModifierList(MODIFIER_LIST, enumDetector, false);

        IElementType declType = parseLocalDeclarationRest(enumDetector.isDetected());

        if (declType != null) {
            decl.done(declType);
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
     *   : "{" (modifiers SimpleName){","} "->" statements "}"
     *   : "{" (type ".")? "(" (modifiers SimpleName (":" type)?){","} ")" (":" type)? "->" expressions "}"
     *   ;
     */
    private void parseFunctionLiteral() {
        parseFunctionLiteral(false);
    }

    private void parseFunctionLiteral(boolean preferBlock) {
        assert _at(LBRACE);

        PsiBuilder.Marker literalExpression = mark();

        PsiBuilder.Marker literal = mark();

        myBuilder.enableNewlines();
        advance(); // LBRACE

        boolean paramsFound = false;

        if (at(ARROW)) {
            //   { -> ...}
            advance(); // ARROW
            mark().done(VALUE_PARAMETER_LIST);
            paramsFound = true;
        }
        else if (at(LPAR)) {
            // Look for ARROW after matching RPAR
            //   {(a, b) -> ...}

            {
                boolean preferParamsToExpressions = isConfirmedParametersByComma();

                PsiBuilder.Marker rollbackMarker = mark();
                parseFunctionLiteralParametersAndType();

                paramsFound = preferParamsToExpressions ?
                                    rollbackOrDrop(rollbackMarker, ARROW, "An -> is expected", RBRACE) :
                                    rollbackOrDropAt(rollbackMarker, ARROW);
            }

            if (!paramsFound) {
                // If not found, try a typeRef DOT and then LPAR .. RPAR ARROW
                //   {((A) -> B).(x) -> ... }
                paramsFound = parseFunctionTypeDotParametersAndType();
            }
        }
        else {
            if (at(IDENTIFIER)) {
                // Try to parse a simple name list followed by an ARROW
                //   {a -> ...}
                //   {a, b -> ...}
                PsiBuilder.Marker rollbackMarker = mark();
                boolean preferParamsToExpressions = (lookahead(1) == COMMA);
                parseFunctionLiteralShorthandParameterList();
                parseOptionalFunctionLiteralType();

                paramsFound = preferParamsToExpressions ?
                              rollbackOrDrop(rollbackMarker, ARROW, "An -> is expected", RBRACE) :
                              rollbackOrDropAt(rollbackMarker, ARROW);
            }
            if (!paramsFound && atSet(JetParsing.TYPE_REF_FIRST)) {
                // Try to parse a type DOT valueParameterList ARROW
                //   {A.(b) -> ...}
                paramsFound = parseFunctionTypeDotParametersAndType();
            }
        }

        if (!paramsFound) {
            if (preferBlock) {
                literal.drop();
                parseStatements();
                expect(RBRACE, "Expecting '}'");
                literalExpression.done(BLOCK);
                myBuilder.restoreNewlinesState();

                return;
            }
        }

        PsiBuilder.Marker body = mark();
        parseStatements();
        body.done(BLOCK);

        expect(RBRACE, "Expecting '}'");
        myBuilder.restoreNewlinesState();

        literal.done(FUNCTION_LITERAL);
        literalExpression.done(FUNCTION_LITERAL_EXPRESSION);
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
                                   JetToken expected, String expectMessage,
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
     * SimpleName{,}
     */
    private void parseFunctionLiteralShorthandParameterList() {
        PsiBuilder.Marker parameterList = mark();

        while (!eof()) {
            PsiBuilder.Marker parameter = mark();

//            int parameterNamePos = matchTokenStreamPredicate(new LastBefore(new At(IDENTIFIER), new AtOffset(doubleArrowPos)));
//            createTruncatedBuilder(parameterNamePos).parseModifierList(MODIFIER_LIST, false);

            expect(IDENTIFIER, "Expecting parameter name", TokenSet.create(ARROW));

            parameter.done(VALUE_PARAMETER);

            if (at(COLON)) {
                PsiBuilder.Marker errorMarker = mark();
                advance(); // COLON
                myJetParsing.parseTypeRef();
                errorMarker.error("To specify a type of a parameter or a return type, use the full notation: {(parameter : Type) : ReturnType -> ...}");
            }
            else if (at(ARROW)) {
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

    // Check that position is followed by top level comma. It can't be expression and we want it be
    // parsed as parameters in function literal
    private boolean isConfirmedParametersByComma() {
        assert _at(LPAR);
        PsiBuilder.Marker lparMarker = mark();
        advance(); // LPAR
        int comma = matchTokenStreamPredicate(new FirstBefore(new At(COMMA), new AtSet(ARROW, RPAR)));
        lparMarker.rollbackTo();
        return comma > 0;
    }

    private boolean parseFunctionTypeDotParametersAndType() {
        PsiBuilder.Marker rollbackMarker = mark();

        // True when it's confirmed that body of literal can't be simple expressions and we prefer to parse
        // it to function params if possible.
        boolean preferParamsToExpressions = false;

        int lastDot = matchTokenStreamPredicate(new LastBefore(new At(DOT), new AtSet(ARROW, RPAR)));
        if (lastDot >= 0) {
            createTruncatedBuilder(lastDot).parseTypeRef();
            if (at(DOT)) {
                advance(); // DOT

                if (at(LPAR)) {
                    preferParamsToExpressions = isConfirmedParametersByComma();
                }

                parseFunctionLiteralParametersAndType();
            }
        }

        return preferParamsToExpressions ?
                      rollbackOrDrop(rollbackMarker, ARROW, "An -> is expected", RBRACE) :
                      rollbackOrDropAt(rollbackMarker, ARROW);
    }

    private void parseFunctionLiteralParametersAndType() {
        parseFunctionLiteralParameterList();
        parseOptionalFunctionLiteralType();
    }

    /*
     * (":" type)?
     */
    private void parseOptionalFunctionLiteralType() {
        if (at(COLON)) {
            advance(); // COLON
            if (at(ARROW)) {
                error("Expecting a type");
            }
            else {
                myJetParsing.parseTypeRef();
            }
        }
    }

    /*
     * "(" (modifiers SimpleName (":" type)?){","} ")"
     */
    private void parseFunctionLiteralParameterList() {
        PsiBuilder.Marker list = mark();
        expect(LPAR, "Expecting a parameter list in parentheses (...)", TokenSet.create(ARROW, COLON));

        myBuilder.disableNewlines();

        if (!at(RPAR)) {
            while (true) {
                if (at(COMMA)) errorAndAdvance("Expecting a parameter declaration");

                PsiBuilder.Marker parameter = mark();
                int parameterNamePos = matchTokenStreamPredicate(new LastBefore(new At(IDENTIFIER), new AtSet(COMMA, RPAR, COLON, ARROW)));
                createTruncatedBuilder(parameterNamePos).parseModifierList(MODIFIER_LIST, false);

                expect(IDENTIFIER, "Expecting parameter declaration");

                if (at(COLON)) {
                    advance(); // COLON
                    myJetParsing.parseTypeRef();
                }
                parameter.done(VALUE_PARAMETER);
                if (!at(COMMA)) break;
                advance(); // COMMA

                if (at(RPAR)) {
                    error("Expecting a parameter declaration");
                    break;
                }
            }
        }

        myBuilder.restoreNewlinesState();

        expect(RPAR, "Expecting ')", TokenSet.create(ARROW, COLON));
        list.done(VALUE_PARAMETER_LIST);
    }

    /*
     * expressions
     *   : SEMI* statement{SEMI+} SEMI*
     */
    public void parseStatements() {
        while (at(SEMICOLON)) advance(); // SEMICOLON
        while (!eof() && !at(RBRACE)) {
            if (!atSet(STATEMENT_FIRST)) {
                errorAndAdvance("Expecting an element");
            }
            if (atSet(STATEMENT_FIRST)) {
                parseStatement();
            }
            if (at(SEMICOLON)) {
                while (at(SEMICOLON)) advance(); // SEMICOLON
            }
            else if (at(RBRACE)) {
                break;
            }
            else if (!myBuilder.newlineBeforeCurrentToken()) {
                errorUntil("Unexpected tokens (use ';' to separate expressions on the same line)", TokenSet.create(EOL_OR_SEMICOLON));
            }
        }
    }

    /*
     * statement
     *  : expression
     *  : declaration
     *  ;
     */
    private void parseStatement() {
        if (!parseLocalDeclaration()) {
            if (!atSet(EXPRESSION_FIRST)) {
                errorAndAdvance("Expecting a statement");
            }
            else {
                parseExpression();
            }
        }
    }

    /*
     * declaration
     *   : function
     *   : property
     *   : extension
     *   : class
     *   : typedef
     *   : object
     *   ;
     */
    private IElementType parseLocalDeclarationRest(boolean isEnum) {
         IElementType keywordToken = tt();
         IElementType declType = null;
         if (keywordToken == CLASS_KEYWORD || keywordToken == TRAIT_KEYWORD) {
             declType = myJetParsing.parseClass(isEnum);
         }
         else if (keywordToken == FUN_KEYWORD) {
             declType = myJetParsing.parseFunction();
         }
         else if (keywordToken == VAL_KEYWORD || keywordToken == VAR_KEYWORD) {
             declType = myJetParsing.parseProperty(true);
         }
         else if (keywordToken == TYPE_KEYWORD) {
             declType = myJetParsing.parseTypeDef();
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
             
             myJetParsing.parseObject(true, true);
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
            parseControlStructureBody();
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

        parseControlStructureBody();

        loop.done(WHILE);
    }

    /*
     * for
     *   : "for" "(" attributes valOrVar? SimpleName (":" type)? "in" element ")" element
     *   ;
     *
     *   TODO: empty loop body (at the end of the block)?
     */
    private void parseFor() {
        assert _at(FOR_KEYWORD);

        PsiBuilder.Marker loop = mark();

        advance(); // FOR_KEYWORD

        myBuilder.disableNewlines();
        expect(LPAR, "Expecting '(' to open a loop range", TokenSet.create(RPAR, VAL_KEYWORD, VAR_KEYWORD, IDENTIFIER));

        PsiBuilder.Marker parameter = mark();
        if (at(VAL_KEYWORD) || at(VAR_KEYWORD)) advance(); // VAL_KEYWORD or VAR_KEYWORD
        if (!myJetParsing.parseIdeTemplate()) {
            expect(IDENTIFIER, "Expecting a variable name", TokenSet.create(COLON));
        }
        if (at(COLON)) {
            advance(); // COLON
            myJetParsing.parseTypeRef();
        }
        parameter.done(LOOP_PARAMETER);

        expect(IN_KEYWORD, "Expecting 'in'");

        PsiBuilder.Marker range = mark();
        parseExpression();
        range.done(LOOP_RANGE);

        expectNoAdvance(RPAR, "Expecting ')'");
        myBuilder.restoreNewlinesState();

        parseControlStructureBody();

        loop.done(FOR);
    }

    /**
     * If it has no ->, it's a block, otherwise a function literal
     */
    private void parseExpressionPreferringBlocks() {
        if (at(LBRACE)) {
            parseFunctionLiteral(true);
        }
        else if (atSet(LABELS) && lookahead(1) == LBRACE ) {
            PsiBuilder.Marker mark = mark();

            parseOperationReference();

            parseFunctionLiteral(true);

            mark.done(PREFIX_EXPRESSION);
        }
        else {
            parseExpression();
        }
    }

    /*
     * element
     */
    private void parseControlStructureBody() {
        PsiBuilder.Marker body = mark();
        if (!at(SEMICOLON)) {
            parseExpressionPreferringBlocks();
        }
        body.done(BODY);
    }

    /*
     * try
     *   : "try" block catchBlock* finallyBlock?
     *   ;
     * catchBlock
     *   : "catch" "(" attributes SimpleName ":" userType ")" block
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

        myJetParsing.parseBlock();

        boolean catchOrFinally = false;
        while (at(CATCH_KEYWORD)) {
            catchOrFinally = true;
            PsiBuilder.Marker catchBlock = mark();
            advance(); // CATCH_KEYWORD

            myJetParsing.parseValueParameterList(false, TokenSet.create(LBRACE, FINALLY_KEYWORD, CATCH_KEYWORD));

            myJetParsing.parseBlock();
            catchBlock.done(CATCH);
        }

        if (at(FINALLY_KEYWORD)) {
            catchOrFinally = true;
            PsiBuilder.Marker finallyBlock = mark();

            advance(); // FINALLY_KEYWORD

            myJetParsing.parseBlock();

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
            parseExpressionPreferringBlocks();
        }
        if (at(SEMICOLON) && lookahead(1) == ELSE_KEYWORD) {
            advance(); // SEMICOLON
        }
        thenBranch.done(THEN);

        if (at(ELSE_KEYWORD)) {
            advance(); // ELSE_KEYWORD

            PsiBuilder.Marker elseBranch = mark();
            if (!at(SEMICOLON)) {
                parseExpressionPreferringBlocks();
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
        expect(LPAR, "Expecting a condition in parentheses '(...)'");

        PsiBuilder.Marker condition = mark();
        parseExpression();
        condition.done(CONDITION);

        expect(RPAR, "Expecting ')");
        myBuilder.restoreNewlinesState();
    }

    /*
     * : "continue" getEntryPoint?
     * : "break" getEntryPoint?
     */
    private void parseJump(JetNodeType type) {
        assert _at(BREAK_KEYWORD) || _at(CONTINUE_KEYWORD);

        PsiBuilder.Marker marker = mark();

        advance(); // BREAK_KEYWORD or CONTINUE_KEYWORD

        parseLabel();

        marker.done(type);
    }

    /*
     * "return" getEntryPoint? element?
     */
    private void parseReturn() {
        assert _at(RETURN_KEYWORD);

        PsiBuilder.Marker returnExpression = mark();

        advance(); // RETURN_KEYWORD

        parseLabel();

        if (atSet(EXPRESSION_FIRST) && !at(EOL_OR_SEMICOLON)) parseExpression();

        returnExpression.done(RETURN);
    }

    /*
     * labels
     */
    private void parseLabel() {
        if (!eol() && atSet(LABELS)) {
            PsiBuilder.Marker labelWrap = mark();

            PsiBuilder.Marker mark = mark();
            advance(); // LABELS
            mark.done(LABEL_REFERENCE);

            labelWrap.done(LABEL_QUALIFIER);
        }
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
     * tupleLiteral
     *   : "#" "(" (((SimpleName "=")? expression){","})? ")"
     *   ;
     */
    private void parseTupleExpression() {
        assert _at(HASH);
        PsiBuilder.Marker mark = mark();

        advance(); // HASH
        advance(); // LPAR
        myBuilder.disableNewlines();
        if (!at(RPAR)) {
            while (true) {
                while (at(COMMA)) {
                    errorAndAdvance("Expecting a tuple entry (element)");
                }

                if (at(IDENTIFIER) && lookahead(1) == EQ) {
                    PsiBuilder.Marker entry = mark();
                    advance(); // IDENTIFIER
                    advance(); // EQ
                    parseExpression();
                    entry.done(LABELED_TUPLE_ENTRY);
                }
                else {
                    parseExpression();
                }

                if (!at(COMMA)) break;
                advance(); // COMMA

                if (at(RPAR)) {
                    error("Expecting a tuple entry (element)");
                    break;
                }
            }

        }
        expect(RPAR, "Expecting ')'");
        myBuilder.restoreNewlinesState();

        mark.done(TUPLE);
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

        parseLabel();

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

            myJetParsing.parseTypeRef();

            if (at(GT)) {
                advance(); // GT
                supertype.drop();
            }
            else {
                supertype.rollbackTo();
            }
            myBuilder.restoreNewlinesState();
        }
        parseLabel();

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
        expect(LPAR, "Expecting an argument list", EXPRESSION_FOLLOW);

        if (!at(RPAR)) {
            while (true) {
                while (at(COMMA)) errorAndAdvance("Expecting an argument");
                parseValueArgument();
                if (!at(COMMA)) break;
                advance(); // COMMA
                if (at(RPAR)) {
                    error("Expecting an argument");
                    break;
                }
            }
        }

        expect(RPAR, "Expecting ')'", EXPRESSION_FOLLOW);
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
        myJetParsing.parseObject(false, false); // Body is not optional because of foo(object : A, B)
        declaration.done(OBJECT_DECLARATION);
        literal.done(OBJECT_LITERAL);
    }

    private void parseOneTokenExpression(JetNodeType type) {
        PsiBuilder.Marker mark = mark();
        advance();
        mark.done(type);
    }

    @Override
    protected JetParsing create(SemanticWhitespaceAwarePsiBuilder builder) {
        return myJetParsing.create(builder);
    }

    private boolean interruptedWithNewLine() {
        return !ALLOW_NEWLINE_OPERATIONS.contains(tt()) && myBuilder.newlineBeforeCurrentToken();
    }
}
