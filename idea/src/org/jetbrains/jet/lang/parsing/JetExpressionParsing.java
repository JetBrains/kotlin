package org.jetbrains.jet.lang.parsing;

import com.intellij.lang.PsiBuilder;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.jet.JetNodeType;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.*;

import static org.jetbrains.jet.JetNodeTypes.*;
import static org.jetbrains.jet.lexer.JetTokens.*;

/**
 * @author abreslav
 */
public class JetExpressionParsing extends AbstractJetParsing {
    private static final TokenSet WHEN_CONDITION_RECOVERY_SET = TokenSet.create(RBRACE, IN_KEYWORD, NOT_IN, IS_KEYWORD, NOT_IS, ELSE_KEYWORD);
    private static final TokenSet WHEN_CONDITION_RECOVERY_SET_WITH_DOUBLE_ARROW = TokenSet.create(RBRACE, IN_KEYWORD, NOT_IN, IS_KEYWORD, NOT_IS, ELSE_KEYWORD, DOUBLE_ARROW, DOT);

    private static final TokenSet TYPE_ARGUMENT_LIST_STOPPERS = TokenSet.create(
            INTEGER_LITERAL, LONG_LITERAL, FLOAT_LITERAL, CHARACTER_LITERAL, STRING_LITERAL, RAW_STRING_LITERAL,
            NAMESPACE_KEYWORD, AS_KEYWORD, TYPE_KEYWORD, CLASS_KEYWORD, THIS_KEYWORD, VAL_KEYWORD, VAR_KEYWORD,
            FUN_KEYWORD, EXTENSION_KEYWORD, FOR_KEYWORD, NULL_KEYWORD, TYPEOF_KEYWORD,
//            NEW_KEYWORD,
            TRUE_KEYWORD, FALSE_KEYWORD, IS_KEYWORD, THROW_KEYWORD, RETURN_KEYWORD, BREAK_KEYWORD,
            CONTINUE_KEYWORD, OBJECT_KEYWORD, IF_KEYWORD, TRY_KEYWORD, ELSE_KEYWORD, WHILE_KEYWORD, DO_KEYWORD,
            WHEN_KEYWORD, RBRACKET, RBRACE, RPAR, PLUSPLUS, MINUSMINUS, MUL, PLUS, MINUS, EXCL, DIV, PERC, LTEQ,
            // TODO GTEQ,   foo<bar, baz>=x
            EQEQEQ, ARROW, DOUBLE_ARROW, EXCLEQEQEQ, EQEQ, EXCLEQ, ANDAND, OROR, SAFE_ACCESS, ELVIS,
            SEMICOLON, RANGE, EQ, MULTEQ, DIVEQ, PERCEQ, PLUSEQ, MINUSEQ, NOT_IN, NOT_IS, HASH,
            COLON
    );

    /*package*/ static final TokenSet EXPRESSION_FIRST = TokenSet.create(
            // Prefix
            MINUS, PLUS, MINUSMINUS, PLUSPLUS, EXCL, LBRACKET, LABEL_IDENTIFIER, AT, ATAT,
            // Atomic

            LPAR, // parenthesized

            // literal constant
            TRUE_KEYWORD, FALSE_KEYWORD,
            STRING_LITERAL, RAW_STRING_LITERAL,
            INTEGER_LITERAL, LONG_LITERAL, CHARACTER_LITERAL, FLOAT_LITERAL,
            NULL_KEYWORD,

            LBRACE, // functionLiteral

            LPAR, // tuple

            THIS_KEYWORD, // this

            IF_KEYWORD, // if
            WHEN_KEYWORD, // when
            TRY_KEYWORD, // try
            TYPEOF_KEYWORD, // typeof
//            NEW_KEYWORD, // new
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

            NAMESPACE_KEYWORD // for absolute qualified names
    );

    private static final TokenSet STATEMENT_FIRST = TokenSet.orSet(
        EXPRESSION_FIRST,
        TokenSet.create(
            // declaration
            LBRACKET, // attribute
            FUN_KEYWORD,
            VAL_KEYWORD, VAR_KEYWORD,
            EXTENSION_KEYWORD,
            CLASS_KEYWORD,
            TYPE_KEYWORD
        ),
        MODIFIER_KEYWORDS
    );

    /*package*/ static final TokenSet EXPRESSION_FOLLOW = TokenSet.create(
            SEMICOLON, DOUBLE_ARROW, COMMA, RBRACE, RPAR, RBRACKET
    );

    @SuppressWarnings({"UnusedDeclaration"})
    private enum Precedence {
        POSTFIX(PLUSPLUS, MINUSMINUS,
                HASH, DOT, SAFE_ACCESS, QUEST), // typeArguments? valueArguments : typeArguments : arrayAccess

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
        assert usedSet.size() == opSet.size();

        usedSet.removeAll(opSet);

        assert usedSet.isEmpty() : "" + usedSet;
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
                for (IElementType type : types) {
                    elvisFollow.add(type);
                }
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

        while (!myBuilder.newlineBeforeCurrentToken() && atSet(precedence.getOperations())) {
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
                myJetParsing.parseAttributeList();
                parsePrefixExpression();
                expression.done(ANNOTATED_EXPRESSION);
            } else {
                return;
            }
        } else if (atSet(Precedence.PREFIX.getOperations())) {
            PsiBuilder.Marker expression = mark();

            parseOperationReference();

            parsePrefixExpression();
            expression.done(PREFIX_EXPRESSION);
        } else {
            parsePostfixExpression();
        }
    }

    /*
     * atomicExpression postfixUnaryOperation?
     *
     * postfixUnaryOperation
     *   : "++" : "--"
     *   : typeArguments? valueArguments (getEntryPoint? functionLiteral)
     *   : typeArguments (getEntryPoint? functionLiteral)
     *   : arrayAccess
     *   : memberAccessOperation postfixUnaryOperation // TODO: Review
     *   ;
     */
    private void parsePostfixExpression() {
//        System.out.println("post at "  + myBuilder.getTokenText());

        PsiBuilder.Marker expression = mark();
        parseAtomicExpression();
        while (true) {
            if (myBuilder.newlineBeforeCurrentToken()) {
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
            else if (at(QUEST)) {
                advance(); // QUEST

                parseCallExpression();

                expression.done(PREDICATE_EXPRESSION);
            }
            else if (at(HASH)) {
                advance(); // HASH

                expect(IDENTIFIER, "Expecting property or function name");

                expression.done(HASH_QUALIFIED_EXPRESSION);
            }
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
            // TODO: be (even) more clever
            int gtPos = matchTokenStreamPredicate(new FirstBefore(
                    new At(GT),
                    new AtSet(TYPE_ARGUMENT_LIST_STOPPERS, TokenSet.create(RPAR, RBRACE, RBRACKET))
                        .or(new AtFirstTokenOfTokens(IDENTIFIER, LPAR)
//                        .or(new AtFirstTokenOfTokens(QUEST, IDENTIFIER))
                        )
            ) {
                @Override
                public boolean isTopLevel(int openAngleBrackets, int openBrackets, int openBraces, int openParentheses) {
                    return openAngleBrackets == 1 && openBrackets == 0 && openBraces == 0 && openParentheses == 0;
                }

                @Override
                public boolean handleUnmatchedClosing(IElementType token) {
                    fail();
                    return true;
                }
            });
            if (gtPos >= 0) {
                myJetParsing.parseTypeArgumentList(gtPos);
                if (!myBuilder.newlineBeforeCurrentToken() && at(LPAR)) parseValueArgumentList();
                parseCallWithClosure();
            }
            else return false;
        }
        else return false;
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
        while (!myBuilder.newlineBeforeCurrentToken()
                && (at(LBRACE)
                    || atSet(LABELS) && lookahead(1) == LBRACE)) {
            if (!at(LBRACE)) {
                assert _atSet(LABELS);
                parsePrefixExpression();
            } else {
                parseFunctionLiteral();
            }
            success = true;
        }
        return success;
    }

    /*
     * atomicExpression
     *   : tupleLiteral // or parenthesized element
     *   : "this" getEntryPoint? ("<" type ">")?
     *   : "typeof" "(" element ")"
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
//        else if (at(NEW_KEYWORD)) {
//            parseNew();
//        }
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
            parseSimpleNameExpression();
        }
        else if (at(IDENTIFIER)) {
            parseSimpleNameExpression();
        }
        else if (at(LBRACE)) {
            parseFunctionLiteral();
        }
        else if (!parseLiteralConstant()) {
            // TODO: better recovery if FIRST(element) did not match
            errorWithRecovery("Expecting an element", EXPRESSION_FOLLOW);
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
     *   : "when" "(" (modifiers "val" SimpleName "=")? element ")" "{"
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
     *   // TODO : consider empty after =>
     *   : whenConditionIf{","} (when  | "=>" element SEMI)
     *   : "else" ("continue" | "=>" element SEMI)
     *   ;
     */
    private void parseWhenEntry() {
        PsiBuilder.Marker entry = mark();

        if (at(ELSE_KEYWORD)) {
            advance(); // ELSE_KEYWORD

            if (!at(CONTINUE_KEYWORD) && !at(DOUBLE_ARROW)) {
                errorUntil("Expecting 'continue' or '=> element'", TokenSet.create(CONTINUE_KEYWORD, DOUBLE_ARROW,
                        RBRACE, EOL_OR_SEMICOLON));
            }

            if (at(DOUBLE_ARROW)) {
                advance(); // DOUBLE_ARROW

                if (atSet(WHEN_CONDITION_RECOVERY_SET)) {
                    error("Expecting an element");
                } else {
                    parseExpression();
                }
            } else if (at(CONTINUE_KEYWORD)) {
                advance(); // CONTINUE_KEYWORD
            } else if (!atSet(WHEN_CONDITION_RECOVERY_SET)) {
                 errorAndAdvance("Expecting 'continue' or '=> element'");
            }
        } else {
            parseWhenEntryNotElse();
        }

        entry.done(WHEN_ENTRY);
        consumeIf(SEMICOLON);
    }

    /*
     * : whenConditionIf{","} (when  | "=>" element SEMI)
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
                error("Expecting an element");
            } else {
                parseExpression();
            }
            // SEMI is consumed in parseWhenEntry
        }
    }

    /*
     * whenConditionIf
     *   : pattern ("if" "(" element ")")?
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
     *   : ("." | "?." postfixExpression typeArguments? valueArguments?
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


            if (atSet(WHEN_CONDITION_RECOVERY_SET_WITH_DOUBLE_ARROW)) {
                error("Expecting an element");
            } else {
                parseExpression();
            }
            condition.done(WHEN_CONDITION_IN_RANGE);
        } else if (at(IS_KEYWORD) || at(NOT_IS)) {
            advance(); // IS_KEYWORD or NOT_IS

            if (atSet(WHEN_CONDITION_RECOVERY_SET_WITH_DOUBLE_ARROW)) {
                error("Expecting a type or a decomposer pattern");
            } else {
                parsePattern();
            }
            condition.done(WHEN_CONDITION_IS_PATTERN);
        } else if (at(DOT) || at(SAFE_ACCESS)) {
            advance(); // DOT or SAFE_ACCESS
            PsiBuilder.Marker mark = mark();
            parsePostfixExpression();
            if (parseCallSuffix()) {
                mark.done(CALL_EXPRESSION);
            }
            else {
                mark.drop();
            }
//            myJetParsing.parseTypeArgumentList();
//            if (at(LPAR)) {
//                parseValueArgumentList();
//            }
            condition.done(WHEN_CONDITION_CALL);
        } else {
            if (atSet(WHEN_CONDITION_RECOVERY_SET_WITH_DOUBLE_ARROW)) {
                error("Expecting an element, is-condition or in-condition");
            } else {
                parseExpression();
            }
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

        myJetParsing.parseAttributeList();

        if (at(NAMESPACE_KEYWORD) || at(IDENTIFIER) || at(LBRACE) || at(THIS_KEYWORD)) {
            PsiBuilder.Marker rollbackMarker = mark();
            parseBinaryExpression(Precedence.ELVIS);
            if (at(AT)) {
                rollbackMarker.drop();
                advance(); // AT
                PsiBuilder.Marker list = mark();
                parseTuplePattern(DECOMPOSER_ARGUMENT);
                list.done(DECOMPOSER_ARGUMENT_LIST);
                pattern.done(DECOMPOSER_PATTERN);
            } else {
                int expressionEndOffset = myBuilder.getCurrentOffset();
                rollbackMarker.rollbackTo();
                rollbackMarker = mark();

                myJetParsing.parseTypeRef();
                if (at(AT)) {
                    errorAndAdvance("'@' is allowed only after a decomposer element, not after a type");
                }
                if (myBuilder.getCurrentOffset() < expressionEndOffset) {
                    rollbackMarker.rollbackTo();
                    parseBinaryExpression(Precedence.ELVIS);
                    pattern.done(DECOMPOSER_PATTERN);
                } else {
                    rollbackMarker.drop();
                    pattern.done(TYPE_PATTERN);
                }
            }
        } else if (at(LPAR)) {
            parseTuplePattern(TUPLE_PATTERN_ENTRY);
            pattern.done(TUPLE_PATTERN);
        }
        else if (at(MUL)) {
            advance(); // MUL
            pattern.done(WILDCARD_PATTERN);
        } else if (at(VAL_KEYWORD)) {
            parseBindingPattern();
            pattern.done(BINDING_PATTERN);
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

        myBuilder.disableNewlines();
        expect(LPAR, "Expecting '('", getDecomposerExpressionFollow());

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
            } else if (at(IN_KEYWORD) || at(NOT_IN)) {
                PsiBuilder.Marker mark = mark();
                advance(); // IN_KEYWORD ot NOT_IN
                mark.done(OPERATION_REFERENCE);

                parseExpression();
                subCondition.done(WHEN_CONDITION_IN_RANGE);
            } else {
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
        } else {
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
        myJetParsing.parseModifierList(MODIFIER_LIST, enumDetector);

        JetNodeType declType = parseLocalDeclarationRest(enumDetector.isDetected());

        if (declType != null) {
            decl.done(declType);
            return true;
        } else {
            decl.rollbackTo();
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

        PsiBuilder.Marker literalExpression = mark();

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
                PsiBuilder.Marker parameterList = mark();
                PsiBuilder.Marker parameter = mark();
                int parameterNamePos = matchTokenStreamPredicate(new LastBefore(new At(IDENTIFIER), new AtOffset(doubleArrowPos)));

                createTruncatedBuilder(parameterNamePos).parseModifierList(MODIFIER_LIST);

                expect(IDENTIFIER, "Expecting parameter name", TokenSet.create(DOUBLE_ARROW));

                parameter.done(VALUE_PARAMETER);

                if (at(COLON)) {
                    errorUntilOffset("To specify a type of a parameter or a return type, use the full notation: {(parameter : Type) : ReturnType => ...}", doubleArrowPos);
                } else if (at(COMMA)) {
                    errorUntilOffset("To specify many parameters, use the full notation: {(p1, p2, ...) => ...}", doubleArrowPos);
                } else if (!at(DOUBLE_ARROW)) {
                    errorUntilOffset("Expecting '=>'", doubleArrowPos);
                }

                parameterList.done(VALUE_PARAMETER_LIST);
            }

            expectNoAdvance(DOUBLE_ARROW, "Expecting '=>'");
        }

        PsiBuilder.Marker body = mark();
        parseStatements();
        body.done(BLOCK);

        expect(RBRACE, "Expecting '}'");
        myBuilder.restoreNewlinesState();

        literal.done(FUNCTION_LITERAL);
        literalExpression.done(FUNCTION_LITERAL_EXPRESSION);
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
                createTruncatedBuilder(parameterNamePos).parseModifierList(MODIFIER_LIST);

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
            } else if (at(RBRACE)) {
                break;
            } else if (!myBuilder.newlineBeforeCurrentToken()) {
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
         else if (keywordToken == OBJECT_KEYWORD) {
             myJetParsing.parseObject(true);
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

        expect(WHILE_KEYWORD, "Expecting 'while' followed by a post-condition");

        parseCondition();

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
     * element
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
            parseExpression();
        }
        if (at(SEMICOLON) && lookahead(1) == ELSE_KEYWORD) {
            advance(); // SEMICOLON
        }
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

//    /*
//     * "new" constructorInvocation // identical to new functionCall
//     *
//     * constructorInvocation
//     *   : userType callSuffix
//     */
//    private void parseNew() {
//        assert _at(NEW_KEYWORD);
//
//        PsiBuilder.Marker creation = mark();
//        advance(); // NEW_KEYWORD
//
//        myJetParsing.parseTypeRef();
//        parseCallSuffix();
//
//        creation.done(NEW);
//    }

    /*
     * "typeof" "(" element ")"
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
     * tupleLiteral // Ambiguity when after a SimpleName (infix call). In this case (e) is treated as an element in parentheses
     *              // to put a tuple, write write ((e))
     *   : "(" ((SimpleName "=")? element){","} ")"
     *   ;
     *
     * element
     *   : "(" element ")"
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
                    errorAndAdvance("Expecting a tuple entry (element)");
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
                    error("Expecting a tuple entry (element)");
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
     * "this" getEntryPoint? ("<" type ">")?
     */
    private void parseThisExpression() {
        assert _at(THIS_KEYWORD);
        PsiBuilder.Marker mark = mark();

        PsiBuilder.Marker thisReference = mark();
        advance(); // THIS_KEYWORD
        thisReference.done(REFERENCE_EXPRESSION);

        parseLabel();

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
     *   : "(" (SimpleName "=")? ("out" | "ref")? element{","} ")"
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
     * (SimpleName "=")? ("out" | "ref")? element
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

    public void parseObjectLiteral() {
        PsiBuilder.Marker literal = mark();
        PsiBuilder.Marker declaration = mark();
        myJetParsing.parseObject(false);
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
}
