package org.jetbrains.jet.lang.parsing;

import com.intellij.lang.PsiBuilder;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.jet.JetNodeType;
import org.jetbrains.jet.lexer.JetTokens;

import static org.jetbrains.jet.JetNodeTypes.*;
import static org.jetbrains.jet.lexer.JetTokens.*;

/**
 * @author abreslav
 */
public class JetExpressionParsing extends AbstractJetParsing {
    private static final TokenSet WHEN_CONDITION_RECOVERY_SET = TokenSet.create(RBRACE, IN_KEYWORD, NOT_IN, IS_KEYWORD, NOT_IS, ELSE_KEYWORD);
    private static final TokenSet WHEN_CONDITION_RECOVERY_SET_WITH_DOUBLE_ARROW = TokenSet.create(RBRACE, IN_KEYWORD, NOT_IN, IS_KEYWORD, NOT_IS, ELSE_KEYWORD, DOUBLE_ARROW);

    private static final TokenSet TYPE_ARGUMENT_LIST_STOPPERS = TokenSet.create(
            INTEGER_LITERAL, LONG_LITERAL, FLOAT_LITERAL, CHARACTER_LITERAL, STRING_LITERAL, RAW_STRING_LITERAL,
            NAMESPACE_KEYWORD, AS_KEYWORD, TYPE_KEYWORD, CLASS_KEYWORD, THIS_KEYWORD, VAL_KEYWORD, VAR_KEYWORD,
            FUN_KEYWORD, EXTENSION_KEYWORD, FOR_KEYWORD, NULL_KEYWORD, TYPEOF_KEYWORD,
            NEW_KEYWORD, TRUE_KEYWORD, FALSE_KEYWORD, IS_KEYWORD, THROW_KEYWORD, RETURN_KEYWORD, BREAK_KEYWORD,
            CONTINUE_KEYWORD, OBJECT_KEYWORD, IF_KEYWORD, TRY_KEYWORD, ELSE_KEYWORD, WHILE_KEYWORD, DO_KEYWORD,
            WHEN_KEYWORD, RBRACKET, RBRACE, RPAR, PLUSPLUS, MINUSMINUS, MUL, PLUS, MINUS, EXCL, DIV, PERC, LTEQ,
            // TODO GTEQ,   foo<bar, baz>=x
            EQEQEQ, ARROW, DOUBLE_ARROW, EXCLEQEQEQ, EQEQ, EXCLEQ, ANDAND, OROR, SAFE_ACCESS, ELVIS, QUEST,
            SEMICOLON, RANGE, EQ, MULTEQ, DIVEQ, PERCEQ, PLUSEQ, MINUSEQ, NOT_IN, NOT_IS, HASH,
            COLON
    );

    private final JetParsing myJetParsing;

    public JetExpressionParsing(SemanticWhitespaceAwarePsiBuilder builder, JetParsing jetParsing) {
        super(builder);
        myJetParsing = jetParsing;
    }

    @SuppressWarnings({"UnusedDeclaration"})
    private enum Precedence {
        POSTFIX(PLUSPLUS, MINUSMINUS), // typeArguments? valueArguments : typeArguments : arrayAccess

        PREFIX(MINUS, PLUS, MINUSMINUS, PLUSPLUS, EXCL) { // attributes

            @Override
            public void parseHigherPrecedence(JetExpressionParsing parser) {
                throw new IllegalStateException("Don't call this method");
            }
        },

        MULTIPLICATIVE(MUL, DIV, PERC) {
            @Override
            public void parseHigherPrecedence(JetExpressionParsing parser) {
                parser.parsePrefixExpression();
            }
        },

        ADDITIVE(PLUS, MINUS),
        RANGE(JetTokens.RANGE),
        SIMPLE_NAME(IDENTIFIER),
        ELVIS(JetTokens.ELVIS),
        WITH_TYPE_RHS(IN_KEYWORD, NOT_IN, AS_KEYWORD, COLON, IS_KEYWORD, NOT_IS) {
            @Override
            public JetNodeType parseRightHandSide(IElementType operation, JetExpressionParsing parser) {
                if (operation == AS_KEYWORD || operation == COLON) {
                    parser.myJetParsing.parseTypeRef();
                    return BINARY_WITH_TYPE;
                }
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
        ARROW(JetTokens.ARROW),
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
         * @param operation the operation sign (e.g. PLUS or IS
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

    /*
     * expression
     *   : attributes expression
     *   : "(" expression ")" // see tupleLiteral
     *   : literalConstant
     *   : functionLiteral
     *   : tupleLiteral
     *   : "null"
     *   : "this" ("<" type ">")?
     *   : expressionWithPrecedences
     *   : if
     *   : try
     *   : "typeof" "(" expression ")"
     *   : "new" constructorInvocation
     *   : objectLiteral
     *   : declaration
     *   : jump
     *   : loop
     *   // block is syntactically equivalent to a functionLiteral with no parameters
     *   ;
     */
    public void parseExpression() {
        // TODO: better recovery for expressions
        if (atSet(RPAR, RBRACE, RBRACKET, GT)) {
            error("Expecting an expression");
            return;
        }
        parseBinaryExpression(Precedence.ASSIGNMENT);
    }

    /*
     * expression (operation expression)*
     *
     * see the precedence table
     */
    private void parseBinaryExpression(Precedence precedence) {
//        System.out.println(precedence.name() + " at " + myBuilder.getTokenText());

        PsiBuilder.Marker expression = mark();

        precedence.parseHigherPrecedence(this);

        while (!myBuilder.newlineBeforeCurrentToken() && atSet(precedence.getOperations())) {
            IElementType operation = tt();
            advance(); // operation

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
                myJetParsing.parseAttributeList();
                parsePrefixExpression();
                expression.done(ANNOTATED_EXPRESSION);
            } else {
                return;
            }
        } else if (atSet(Precedence.PREFIX.getOperations())) {
            PsiBuilder.Marker expression = mark();
            advance(); // operation

            parsePrefixExpression();
            expression.done(PREFIX_EXPRESSION);
        } else {
            parsePostfixExpression();
        }
    }

    /*
     * expression operation?
     */
    private void parsePostfixExpression() {
//        System.out.println("post at "  + myBuilder.getTokenText());

        PsiBuilder.Marker expression = mark();
        parseAtomicExpression();
        while (true) {
            if (myBuilder.newlineBeforeCurrentToken()) {
                break;
            } else if (at(LBRACKET)) {
                parseArrayAccess();
                expression.done(ARRAY_ACCESS_EXPRESSION);
            } else if (atSet(Precedence.POSTFIX.getOperations())) {
                advance(); // operation
                expression.done(POSTFIX_EXPRESSION);
            } else if (parseCallWithClosure()) {
                expression.done(CALL_EXPRESSION);
            } else if (at(LPAR)) {
                parseValueArgumentList();
                parseCallWithClosure();
                expression.done(CALL_EXPRESSION);
            } else if (at(LT)) {
                // TODO: be (even) more clever
                int gtPos = matchTokenStreamPredicate(new FirstBefore(new At(GT), new AtSet(TYPE_ARGUMENT_LIST_STOPPERS, false)) {
                    @Override
                    public boolean isTopLevel(int openAngleBrackets, int openBrackets, int openBraces, int openParentheses) {
                        return openAngleBrackets == 1 && openBrackets == 0 && openBraces == 0 && openParentheses == 0;
                    }
                });
                if (gtPos >= 0) {
                    myJetParsing.parseTypeArgumentList();
                    if (!myBuilder.newlineBeforeCurrentToken() && at(LPAR)) parseValueArgumentList();
                    parseCallWithClosure();
                    expression.done(CALL_EXPRESSION);
                } else {
                    break;
                }
            } else if (at(DOT)) {
                advance(); // DOT

                parseAtomicExpression();

                expression.done(DOT_QIALIFIED_EXPRESSION);
            } else if (at(SAFE_ACCESS)) {
                advance(); // SAFE_ACCESS

                parseAtomicExpression();

                expression.done(SAFE_ACCESS_EXPRESSION);
            } else if (at(HASH)) {
                advance(); // HASH

                expect(IDENTIFIER, "Expecting property or function name");

                expression.done(HASH_QIALIFIED_EXPRESSION);
            } else {
                break;
            }
            expression = expression.precede();
        }
        expression.drop();
    }

    /*
     * expression functionLiteral?
     */
    protected boolean parseCallWithClosure() {
        if (!myBuilder.newlineBeforeCurrentToken() && at(LBRACE)) {
            parseFunctionLiteral();
            return true;
        }
        return false;
    }

    /*
     * atomicExpression
     *   : tupleLiteral // or parenthesized expression
     *   : "this" ("<" type ">")?
     *   : "typeof" "(" expression ")"
     *   : "new" constructorInvocation
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
     *   : "namespace" // foo the root namespace
     *   ;
     */
    private void parseAtomicExpression() {
//        System.out.println("atom at "  + myBuilder.getTokenText());

        if (at(LPAR)) {
            parseParenthesizedExpressionOrTuple();
        }
        else if (at(NAMESPACE_KEYWORD)) {
            parseOneTokenExpression(ROOT_NAMESPACE);
        }
        else if (at(THIS_KEYWORD)) {
            parseThisExpression();
        }
        else if (at(TYPEOF_KEYWORD)) {
            parseTypeOf();
        }
        else if (at(NEW_KEYWORD)) {
            parseNew();
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
        else if (atSet(CLASS_KEYWORD, EXTENSION_KEYWORD, FUN_KEYWORD, VAL_KEYWORD,
                VAR_KEYWORD, TYPE_KEYWORD)) {
            parseLocalDeclaration();
        }
        else if (at(FIELD_IDENTIFIER)) {
            parseOneTokenExpression(REFERENCE_EXPRESSION);
        }
        else if (at(IDENTIFIER)) {
            if (JetParsing.MODIFIER_KEYWORD_MAP.containsKey(myBuilder.getTokenText())) {
                if (!parseLocalDeclaration()) {
                    parseSimpleNameExpression();
                }
            } else {
                parseSimpleNameExpression();
            }
        }
        else if (at(LBRACE)) {
            parseFunctionLiteral();
        }
        else if (!parseLiteralConstant()) {
            // TODO: better recovery if FIRST(expression) did not match
            errorAndAdvance("Expecting an expression");
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
        else if (at(STRING_LITERAL) || at(RAW_STRING_LITERAL)) {
            parseOneTokenExpression(STRING_CONSTANT);
        }
        else if (at(INTEGER_LITERAL)) {
            parseOneTokenExpression(INTEGER_CONSTANT);
        }
        else if (at(LONG_LITERAL)) {
            parseOneTokenExpression(LONG_CONSTANT);
        }
        else if (at(CHARACTER_LITERAL)) {
            parseOneTokenExpression(CHARACTER_CONSTANT);
        }
        else if (at(FLOAT_LITERAL)) {
            parseOneTokenExpression(FLOAT_CONSTANT);
        }
        else if (at(NULL_KEYWORD)) {
            parseOneTokenExpression(NULL);
        } else {
            return false;
        }
        return true;
    }

    /*
     * when
     *   : "when" "(" (modifiers "val" SimpleName "=")? expression ")" "{"
     *         whenEntry*
     *     "}"
     *   ;
     */
    private void parseWhen() {
        assert _at(WHEN_KEYWORD);

        PsiBuilder.Marker when = mark();

        advance(); // WHEN_KEYWORD

        myBuilder.disableNewlines();
        expect(LPAR, "Expecting '('");

        int valPos = matchTokenStreamPredicate(new FirstBefore(new At(VAL_KEYWORD), new AtSet(RPAR, LBRACE, RBRACE, SEMICOLON, EQ)));
        if (valPos >= 0) {
            myJetParsing.parseProperty(true);
        } else {
            parseExpression();
        }

        expect(RPAR, "Expecting ')'");
        myBuilder.restoreNewlinesState();

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
     *   : whenConditionIf{","} (when  | "=>" expression SEMI)
     *   : "else" ("continue" | "=>" expression SEMI)
     *   ;
     */
    private void parseWhenEntry() {
        PsiBuilder.Marker entry = mark();

        if (at(ELSE_KEYWORD)) {
            advance(); // ELSE_KEYWORD

            if (!at(CONTINUE_KEYWORD) && !at(DOUBLE_ARROW)) {
                errorUntil("Expecting 'continue' or '=> expression'", TokenSet.create(CONTINUE_KEYWORD, DOUBLE_ARROW,
                        RBRACE, EOL_OR_SEMICOLON));
            }

            if (at(DOUBLE_ARROW)) {
                advance(); // DOUBLE_ARROW

                if (atSet(WHEN_CONDITION_RECOVERY_SET)) {
                    error("Expecting an expression");
                } else {
                    parseExpression();
                }
            } else if (at(CONTINUE_KEYWORD)) {
                advance(); // CONTINUE_KEYWORD
            } else if (!atSet(WHEN_CONDITION_RECOVERY_SET)) {
                 errorAndAdvance("Expecting 'continue' or '=> expression'");
            }
        } else {
            parseWhenEntryNotElse();
        }

        entry.done(WHEN_ENTRY);
        consumeIf(SEMICOLON);
    }

    /*
     * : whenConditionIf{","} (when  | "=>" expression SEMI)
     */
    private void parseWhenEntryNotElse() {
        while (true) {
            while (at(COMMA)) errorAndAdvance("Expecting a when-condition");
            parseWhenConditionIf();
            if (!at(COMMA)) break;
            advance(); // COMMA
        }
        if (at(WHEN_KEYWORD)) {
            parseWhen();
        } else {
            expect(DOUBLE_ARROW, "Expecting '=>' or 'when'", WHEN_CONDITION_RECOVERY_SET);
            if (atSet(WHEN_CONDITION_RECOVERY_SET)) {
                error("Expecting an expression");
            } else {
                parseExpression();
            }
            // SEMI is consumed in parseWhenEntry
        }
    }

    /*
     * whenConditionIf
     *   : pattern ("if" "(" expression ")")?
     *   ;
     */
    private void parseWhenConditionIf() {
        parseWhenCondition();

        if (at(IF_KEYWORD)) {
            advance(); // IF_KEYWORD

            // TODO : allow omitting these parentheses
            myBuilder.disableNewlines();
            expect(LPAR, "Expecting '('");

            parseExpression();

            expect(RPAR, "Expecting ')'");
            myBuilder.restoreNewlinesState();
        }
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
            advance(); // IN_KEYWORD or NOT_IN

            if (atSet(WHEN_CONDITION_RECOVERY_SET_WITH_DOUBLE_ARROW)) {
                error("Expecting an expression");
            } else {
                parseExpression();
            }
        } else if (at(IS_KEYWORD) || at(NOT_IS)) {
            advance(); // IS_KEYWORD or NOT_IS

            if (atSet(WHEN_CONDITION_RECOVERY_SET_WITH_DOUBLE_ARROW)) {
                error("Expecting a type or a decomposer pattern");
            } else {
                parsePattern();
            }
        } else {
            if (atSet(WHEN_CONDITION_RECOVERY_SET_WITH_DOUBLE_ARROW)) {
                error("Expecting an expression, is-condition or in-condition");
            } else {
                parseExpression();
            }
        }
        myBuilder.restoreNewlinesState();
        condition.done(WHEN_CONDITION);
    }

    /*
     * pattern
     *  : attributes pattern
     *  : type // '[a] T' is a type-pattern 'T' with an attribute '[a]', not a type-pattern '[a] T'
     *         // this makes sense because is-chack may be different for a type with attributes
     *  : tuplePattern
     *  : decomposerPattern
     *  : constantPattern
     *  : bindingPattern
     *  : expressionPattern
     *   ;
     */
    private void parsePattern() {
        PsiBuilder.Marker pattern = mark();

        myJetParsing.parseAttributeList();

        if (at(NAMESPACE_KEYWORD) || at(IDENTIFIER)) {
            myJetParsing.parseUserTypeOrQualifiedName();
            if (!myBuilder.newlineBeforeCurrentToken() && at(LPAR)) {
                PsiBuilder.Marker list = mark();
                parseTuplePattern(DECOMPOSER_ARGUMENT);
                list.done(DECOMPOSER_ARGUMENT_LIST);
                pattern.done(DECOMPOSER_PATTERN);
            } else {
                pattern.done(TYPE_PATTERN);
            }
        } else if (at(LPAR)) {
            parseTuplePattern(TUPLE_PATTERN_ENTRY);
            pattern.done(TUPLE_PATTERN);
        } else if (at(LBRACE) || at(CAPITALIZED_THIS_KEYWORD)) {
            myJetParsing.parseTypeRef();
            pattern.done(TYPE_PATTERN);
        }
        else if (at(QUEST)) {
            parseBindingPattern();
            pattern.done(BINDING_PATTERN);
        } else if (at(EQ)) {
            advance(); // EQ
            parseExpression();
            pattern.done(EXPRESSION_PATTERN);
        } else if (parseLiteralConstant()) {
            pattern.done(EXPRESSION_PATTERN);
        } else {
            errorUntil("Pattern expected", TokenSet.create(RBRACE, DOUBLE_ARROW));
            pattern.drop();
        }
    }

    /*
     * tuplePattern
     *  : "(" ((SimpleName "=")? pattern){","}? ")"
     *  ;
     */
    private void parseTuplePattern(JetNodeType entryType) {
        assert _at(LPAR);

        myBuilder.disableNewlines();
        advance(); // LPAR

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
     *   : "?" SimpleName? binding?
     *   ;
     *
     * binding
     *   : "is" pattern
     *   : "!is" pattern
     *   : "in" expression
     *   : "!in" expression
     *   : "=" expression
     *   ;
     */
    private void parseBindingPattern() {
        assert _at(QUEST);

        advance(); // QUEST

        consumeIf(IDENTIFIER);

        if (at(IS_KEYWORD) || at(NOT_IS)) {
            advance(); // IS_KEYWORD or NOT_IS

            parsePattern();
        } else if (at(IN_KEYWORD) || at(NOT_IN)) {
            advance(); // IN_KEYWORD ot NOT_IN

            parseExpression();
        } else if (at(EQ)) {
            advance(); // EQ

            parseExpression();
        }
    }

    /*
     * qualifiedName
     *   : ("namespace" ".")? SimpleName{","}
     *   ;
     */
    private void parseQualifiedName() {
        PsiBuilder.Marker mark = mark();

        if (at(NAMESPACE_KEYWORD)) {
            advance(); // NAMESPACE_KEYWORD
            mark.done(REFERENCE_EXPRESSION);
            mark = mark.precede();

            expect(DOT, "Expecting '.'");
        }
        while (true) {
            expect(IDENTIFIER, "Expecting an indetifier", TokenSet.create(DOT));
            mark.done(REFERENCE_EXPRESSION);
            mark = mark.precede();
            if (!at(DOT)) break;
            advance(); // DOT
        }
        mark.drop();
    }

    /*
     * arrayAccess
     *   : "[" expression{","} "]"
     *   ;
     */
    private void parseArrayAccess() {
        assert _at(LBRACKET);

        PsiBuilder.Marker indices = mark();

        myBuilder.disableNewlines();
        advance(); // LBRACKET

        while (true) {
            if (at(COMMA)) errorAndAdvance("Expecting an index expression");
            if (at(RBRACKET)) {
                error("Expecting an index expression");
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
        expect(IDENTIFIER, "Expecting an identifier [Interal error]");
        simpleName.done(REFERENCE_EXPRESSION);
    }

    /*
     * modifiers declarationRest
     */
    private boolean parseLocalDeclaration() {
        PsiBuilder.Marker decls = mark();
        JetParsing.EnumDetector enumDetector = new JetParsing.EnumDetector();
        myJetParsing.parseModifierList(enumDetector);

        JetNodeType declType = parseLocalDeclarationRest(enumDetector.isEnum());

        if (declType != null) {
            decls.done(declType);
            return true;
        } else {
            decls.rollbackTo();
            return false;
        }
    }

    /*
     * functionLiteral  // one can use "it" as a parameter name
     *   : "{" expressions "}"
     *   : "{" (type ".")? modifiers SimpleName "=>" expressions "}"
     *   : "{" (type ".")? "(" (modifiers SimpleName (":" type)?){","} ")" (":" type)? "=>" expressions "}"
     *   ;
     */
    private void parseFunctionLiteral() {
        assert _at(LBRACE);

        PsiBuilder.Marker literal = mark();

        myBuilder.enableNewlines();
        advance(); // LBRACE

        int doubleArrowPos = matchTokenStreamPredicate(new FirstBefore(new At(DOUBLE_ARROW), new At(RBRACE)) {
            @Override
            public boolean isTopLevel(int openAngleBrackets, int openBrackets, int openBraces, int openParentheses) {
                return openBraces == 0;
            }
        });

        if (doubleArrowPos >= 0) {
            boolean dontExpectParameters = false;

            int lastDot = matchTokenStreamPredicate(new LastBefore(new At(DOT), new AtOffset(doubleArrowPos)));
            if (lastDot >= 0) { // There is a receiver type
                createTruncatedBuilder(lastDot).parseTypeRef();

                expect(DOT, "Expecting '.'");

                if (!at(LPAR)) {
                    int firstLParPos = matchTokenStreamPredicate(new FirstBefore(new At(LPAR), new AtOffset(doubleArrowPos)));

                    if (firstLParPos >= 0) {
                        errorUntilOffset("Expecting '('", firstLParPos);
                    } else {
                        errorUntilOffset("To specify a receiver type, use the full notation: {ReceiverType.(parameters) [: ReturnType] => ...}",
                            doubleArrowPos);
                        dontExpectParameters = true;
                    }
                }

            }

            if (at(LPAR)) {
                parseFunctionLiteralParameterList();

                if (at(COLON)) {
                    advance(); // COLON
                    if (at(DOUBLE_ARROW)) {
                        error("Expecting a type");
                    } else {
                        myJetParsing.parseTypeRef();
                    }
                }
            } else if (!dontExpectParameters) {
                PsiBuilder.Marker parameter = mark();
                int parameterNamePos = matchTokenStreamPredicate(new LastBefore(new At(IDENTIFIER), new AtOffset(doubleArrowPos)));

                createTruncatedBuilder(parameterNamePos).parseModifierList();

                expect(IDENTIFIER, "Expecting parameter name", TokenSet.create(DOUBLE_ARROW));

                parameter.done(VALUE_PARAMETER);

                if (at(COLON)) {
                    errorUntilOffset("To specify a type of a parameter or a return type, use the full notation: {(parameter : Type) : ReturnType => ...}", doubleArrowPos);
                } else if (at(COMMA)) {
                    errorUntilOffset("To specify many parameters, use the full notation: {(p1, p2, ...) => ...}", doubleArrowPos);
                } else if (!at(DOUBLE_ARROW)) {
                    errorUntilOffset("Expecting '=>'", doubleArrowPos);
                }

            }

            expectNoAdvance(DOUBLE_ARROW, "Expecting '=>'");
        }

        PsiBuilder.Marker body = mark();
        parseExpressions();
        body.done(BODY);

        expect(RBRACE, "Expecting '}'");
        myBuilder.restoreNewlinesState();

        literal.done(FUNCTION_LITERAL);
    }

    /*
     * "(" (modifiers SimpleName (":" type)?){","} ")"
     */
    private void parseFunctionLiteralParameterList() {
        PsiBuilder.Marker list = mark();
        expect(LPAR, "Expecting a parameter list in parentheses (...)", TokenSet.create(DOUBLE_ARROW, COLON));

        myBuilder.disableNewlines();

        if (!at(RPAR)) {
            while (true) {
                if (at(COMMA)) errorAndAdvance("Expecting a parameter declaration");

                PsiBuilder.Marker parameter = mark();
                int parameterNamePos = matchTokenStreamPredicate(new LastBefore(new At(IDENTIFIER), new AtSet(COMMA, RPAR, COLON, DOUBLE_ARROW)));
                createTruncatedBuilder(parameterNamePos).parseModifierList();

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

        expect(RPAR, "Expecting ')", TokenSet.create(DOUBLE_ARROW, COLON));
        list.done(VALUE_PARAMETER_LIST);
    }

    /*
     * expressions
     *   : SEMI* expression{SEMI+} SEMI*
     */
    public void parseExpressions() {
        while (at(SEMICOLON)) advance(); // SEMICOLON
        while (!eof() && !at(RBRACE)) {
            parseExpression();
            if (at(SEMICOLON)) {
                while (at(SEMICOLON)) advance(); // SEMICOLON
            } else if (at(RBRACE)) {
                break;
            } else if (!myBuilder.newlineBeforeCurrentToken()) {
                errorUntil("Unexpected tokens (use ';' to separate expressions on the same line", TokenSet.create(EOL_OR_SEMICOLON));
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
     *   ;
     */
    private JetNodeType parseLocalDeclarationRest(boolean isEnum) {
         IElementType keywordToken = tt();
         JetNodeType declType = null;
         if (keywordToken == CLASS_KEYWORD) {
             declType = myJetParsing.parseClass(isEnum);
         }
         else if (keywordToken == EXTENSION_KEYWORD) {
             declType = myJetParsing.parseExtension();
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
         return declType;
     }

    /*
     * doWhile
     *   : "do" expression "while" "(" expression ")"
     *   ;
     */
    private void parseDoWhile() {
        assert _at(DO_KEYWORD);

        PsiBuilder.Marker loop = mark();

        advance(); // DO_KEYWORD

        if (!at(WHILE_KEYWORD)) {
            parseControlStructureBody();
        }

        expect(WHILE_KEYWORD, "Expecting 'while' followed by a post-condition");

        parseCondition();

        loop.done(DO_WHILE);
    }

    /*
     * while
     *   : "while" "(" expression ")" expression
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
     *   : "for" "(" attributes valOrVar? SimpleName (":" type)? "in" expression ")" expression
     *   ;
     */
    private void parseFor() {
        assert _at(FOR_KEYWORD);

        PsiBuilder.Marker loop = mark();

        advance(); // FOR_KEYWORD

        myBuilder.disableNewlines();
        expect(LPAR, "Expecting '(' to open a loop range", TokenSet.create(RPAR, VAL_KEYWORD, VAR_KEYWORD, IDENTIFIER));

        PsiBuilder.Marker parameter = mark();
        if (at(VAL_KEYWORD) || at(VAR_KEYWORD)) advance(); // VAL_KEYWORD or VAR_KEYWORD
        expect(IDENTIFIER, "Expecting a variable name", TokenSet.create(COLON));
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

    /*
     * expression
     */
    private void parseControlStructureBody() {
        PsiBuilder.Marker body = mark();
        if (!at(SEMICOLON)) {
            parseExpression();
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

        while (at(CATCH_KEYWORD)) {
            PsiBuilder.Marker catchBlock = mark();
            advance(); // CATCH_KEYWORD

            myJetParsing.parseValueParameterList(false, TokenSet.create(LBRACE, FINALLY_KEYWORD, CATCH_KEYWORD));

            myJetParsing.parseBlock();
            catchBlock.done(CATCH);
        }

        if (at(FINALLY_KEYWORD)) {
            PsiBuilder.Marker finallyBlock = mark();

            advance(); // FINALLY_KEYWORD

            myJetParsing.parseBlock();

            finallyBlock.done(FINALLY);
        }

        tryExpression.done(TRY);
    }

    /*
     * if
     *   : "if" "(" expression ")" expression SEMI? ("else" expression)?
     *   ;
     */
    private void parseIf() {
        assert _at(IF_KEYWORD);

        PsiBuilder.Marker marker = mark();

        advance(); //IF_KEYWORD

        parseCondition();

        PsiBuilder.Marker thenBranch = mark();
        if (!at(ELSE_KEYWORD) && !at(SEMICOLON)) {
            parseExpression();
        }
        consumeIf(SEMICOLON);
        thenBranch.done(THEN);

        if (at(ELSE_KEYWORD)) {
            advance(); // ELSE_KEYWORD

            PsiBuilder.Marker elseBranch = mark();
            if (!at(SEMICOLON)) {
                parseExpression();
            }
            elseBranch.done(ELSE);
        }

        marker.done(IF);
    }

    /*
     * "(" expression ")"
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
     * : "continue" stringLiteral?
     * : "break" stringLiteral?
     *
     * stringLiteral
     *   : StringWithTemplates
     *   : NoEscapeString
     *   ;
     */
    private void parseJump(JetNodeType type) {
        assert _at(BREAK_KEYWORD) || _at(CONTINUE_KEYWORD);

        PsiBuilder.Marker marker = mark();

        advance(); // BREAK_KEYWORD or CONTINUE_KEYWORD

        if (!eol() && (at(RAW_STRING_LITERAL) || at(STRING_LITERAL))) advance(); // RAW_STRING_LITERAL or STRING_LITERAL

        marker.done(type);
    }

    /*
     * "return" expression?
     */
    private void parseReturn() {
        assert _at(RETURN_KEYWORD);

        PsiBuilder.Marker returnExpression = mark();

        advance(); // RETURN_KEYWORD

        if (!at(EOL_OR_SEMICOLON)) parseExpression();

        returnExpression.done(RETURN);
    }

    /*
     * : "throw" expression
     */
    private void parseThrow() {
        assert _at(THROW_KEYWORD);

        PsiBuilder.Marker marker = mark();

        advance(); // THROW_KEYWORD

        parseExpression();

        marker.done(THROW);
    }

    /*
     * "new" constructorInvocation // identical to new nunctionCall
     *
     * constructorInvocation
     *   : userType valueArguments?
     */
    private void parseNew() {
        assert _at(NEW_KEYWORD);

        PsiBuilder.Marker creation = mark();
        advance(); // NEW_KEYWORD

        myJetParsing.parseTypeRef();

        if (!eol() && at(LPAR)) {
            parseValueArgumentList();
        }
        creation.done(NEW);
    }

    /*
     * "typeof" "(" expression ")"
     */
    private void parseTypeOf() {
        assert _at(TYPEOF_KEYWORD);

        PsiBuilder.Marker typeof = mark();
        advance(); // TYPEOF_KEYWORD

        myBuilder.disableNewlines();
        expect(LPAR, "Expecting '('");

        parseExpression();

        expect(RPAR, "Expecting ')'");
        myBuilder.restoreNewlinesState();

        typeof.done(TYPEOF);
    }

    /*
     * tupleLiteral // Ambiguity when after a SimpleName (infix call). In this case (e) is treated as an expression in parentheses
     *              // to put a tuple, write write ((e))
     *   : "(" ((SimpleName "=")? expression){","} ")"
     *   ;
     *
     * expression
     *   : "(" expression ")"
     *   ;
     *
     * TODO: duplication with valueArguments (but for the error messages)
     */
    private void parseParenthesizedExpressionOrTuple() {
        assert _at(LPAR);

        PsiBuilder.Marker mark = mark();

        myBuilder.disableNewlines();
        advance(); // LPAR
        boolean tuple = false;
        if (!at(RPAR)) {
            while (true) {
                while (at(COMMA)) {
                    tuple = true;
                    errorAndAdvance("Expecting a tuple entry (expression)");
                }

                if (at(IDENTIFIER) && lookahead(1) == EQ) {
                    PsiBuilder.Marker entry = mark();
                    advance(); // IDENTIFIER
                    advance(); // EQ
                    tuple = true;
                    parseExpression();
                    entry.done(LABELED_TUPLE_ENTRY);
                } else {
                    parseExpression();
                }

                if (!at(COMMA)) break;
                advance(); // COMMA
                tuple = true;

                if (at(RPAR)) {
                    error("Expecting a tuple entry (expression)");
                    break;
                }
            }
        } else {
            tuple = true;
        }

        expect(RPAR, "Expecting ')'");
        myBuilder.restoreNewlinesState();

        mark.done(tuple ? TUPLE : PARENTHESIZED);
    }

    /*
     * "this" ("<" type ">")?
     */
    private void parseThisExpression() {
        assert _at(THIS_KEYWORD);
        PsiBuilder.Marker mark = mark();
        advance(); // THIS_KEYWORD
        if (at(LT)) {
            // This may be "this < foo" or "this<foo>", thus the backtracking
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
        mark.done(THIS_EXPRESSION);
    }

    /*
     * valueArguments
     *   : "(" (SimpleName "=")? ("out" | "ref")? expression{","} ")"
     *   ;
     */
    public void parseValueArgumentList() {
        PsiBuilder.Marker list = mark();

        myBuilder.disableNewlines();
        expect(LPAR, "Expecting an argument list", TokenSet.create(RPAR));

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

        expect(RPAR, "Expecting ')'");
        myBuilder.restoreNewlinesState();

        list.done(VALUE_ARGUMENT_LIST);
    }

    /*
     * (SimpleName "=")? ("out" | "ref")? expression
     */
    private void parseValueArgument() {
        PsiBuilder.Marker argument = mark();
        if (at(IDENTIFIER) && lookahead(1) == EQ) {
            advance(); // IDENTIFIER
            advance(); // EQ
        }
        if (at(OUT_KEYWORD) || at(REF_KEYWORD)) advance(); // REF or OUT
        parseExpression();
        argument.done(VALUE_ARGUMENT);
    }

    /*
     * objectLiteral
     *   : "object" delegationSpecifier{","}? classBody // Cannot make class body optional: foo(object F, a)
     *   ;
     */
    public void parseObjectLiteral() {
        assert _at(OBJECT_KEYWORD);

        PsiBuilder.Marker literal = mark();

        advance(); // OBJECT_KEYWORD

        if (at(LBRACE)) {
            myJetParsing.parseClassBody();
        }
        else {
            myJetParsing.parseDelegationSpecifierList();
            if (at(LBRACE)) {
                myJetParsing.parseClassBody();
            }
        }
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
}
