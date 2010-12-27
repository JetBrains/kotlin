package org.jetbrains.jet.lang.parsing;

import com.intellij.lang.PsiBuilder;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.jet.JetNodeType;

import static org.jetbrains.jet.JetNodeTypes.*;
import static org.jetbrains.jet.lexer.JetTokens.*;

/**
 * @author abreslav
 */
public class JetExpressionParsing extends AbstractJetParsing {
    private final JetParsing myJetParsing;

    public JetExpressionParsing(SemanticWhitespaceAwarePsiBuilder builder, JetParsing jetParsing) {
        super(builder);
        myJetParsing = jetParsing;
    }

    /*
     * expression
     *   : "(" expression ")" // see tupleLiteral
     *   : literalConstant
     *   : functionLiteral
     *   : tupleLiteral
     *   : listLiteral
     *   : mapLiteral
     *   : range
     *   : "null"
     *   : "this" ("<" type ">")?
     *   : expressionWithPrecedences
     *   : match
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
        if (at(LPAR)) {
            parseParenthesizedExpressionOrTuple();
        }
        else if (at(LBRACKET)) {
            parseMapListOrRange();
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
            // TODO
        }
        else if (at(TRY_KEYWORD)) {
            // TODO
        }
        else if (at(FOR_KEYWORD)) {
            // TODO
        }
        else if (at(WHILE_KEYWORD)) {
            // TODO
        }
        else if (at(DO_KEYWORD)) {
            // TODO
        }
        else if (atSet(TokenSet.create(
                CLASS_KEYWORD,
                EXTENSION_KEYWORD,
                FUN_KEYWORD,
                VAL_KEYWORD,
                VAR_KEYWORD,
                TYPE_KEYWORD))) {
            // TODO
        }
        else if (at(IDENTIFIER)) {
            advance(); // TODO
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
        else if (at(STRING_LITERAL) || at(RAW_STRING_LITERAL)) {
            parseOneTokenExpression(STRING_CONSTANT);
        }
        else if (at(TRUE_KEYWORD)) {
            parseOneTokenExpression(BOOLEAN_CONSTANT);
        }
        else if (at(FALSE_KEYWORD)) {
            parseOneTokenExpression(BOOLEAN_CONSTANT);
        }
        else if (at(NULL_KEYWORD)) {
            parseOneTokenExpression(NULL);
        }
    }

    /*
     * : "continue" SimpleName
     * : "break" SimpleName
     */
    private void parseJump(JetNodeType type) {
        assert at(BREAK_KEYWORD) || at(CONTINUE_KEYWORD);

        PsiBuilder.Marker marker = mark();

        advance(); // BREAK_KEYWORD or CONTINUE_KEYWORD

        if (!eol() && at(IDENTIFIER)) advance(); // IDENTIFIER

        marker.done(type);
    }

    /*
     * "return" expression?
     */
    private void parseReturn() {
        assert at(RETURN_KEYWORD);

        PsiBuilder.Marker returnExpression = mark();

        advance(); // RETURN_KEYWORD

        if (!at(EOL_OR_SEMICOLON)) parseExpression();

        returnExpression.done(RETURN);
    }

    /*
     * : "throw" expression
     */
    private void parseThrow() {
        assert at(THROW_KEYWORD);

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
        assert at(NEW_KEYWORD);

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
        assert at(TYPEOF_KEYWORD);

        PsiBuilder.Marker typeof = mark();
        advance(); // TYPEOF_KEYWORD

        expect(LPAR, "Expecting '('");
        parseExpression();
        expect(RPAR, "Expecting ')'");
        typeof.done(TYPEOF);
    }

    /*
     * listLiteral
     *   : "[" expression{","}? "]"
     *   ;
     *
     * mapLiteral
     *   : "[" mapEntryLiteral{","} "]"
     *   : "[" ":" "]"
     *   ;
     *
     * mapEntryLiteral
     *   : expression ":" expression
     *   ;
     *
     * range
     *   : "[" expression ".." expression "]"
     *   ;
     */
    private void parseMapListOrRange() {
        assert at(LBRACKET);

        PsiBuilder.Marker literal = mark();

        advance(); // LBRACKET

        // If this is an emty map "[:]"
        if (at(COLON)) {
            advance(); // COLON
            expect(RBRACKET, "Expecting ']' to close an empty map literal '[:]'");
            literal.done(MAP_LITERAL);
            return;
        }

        PsiBuilder.Marker item = mark();
        parseExpression();

        // If it is a map "[e:e, e:e]"
        if (at(COLON)) {
            advance(); // COLON

            parseExpression();
            item.done(MAP_LITERAL_ENTRY);

            while (at(COMMA)) {
                advance(); // COMMA
                if (at(COMMA)) error("Expecting a map entry");
                parseMapLiteralEntry();
            }
            expect(RBRACKET, "Expecting ']' to close a map literal");
            literal.done(MAP_LITERAL);
            return;
        }

        // If it is a range "[a..b]"
        if (at(RANGE)) {
            item.drop();
            advance(); // RANGE

            parseExpression();

            expect(RBRACKET, "Expecting ']' to close the range");
            literal.done(RANGE_LITERAL);
            return;
        }

        // Else: it must be a list literal "[a, b, c]"
        item.drop();
        while (at(COMMA)) {
            advance(); // COMMA
            if (at(COMMA)) error("Expecting a list entry");
            parseExpression();
        }

        expect(RBRACKET, "Expecting a ']' to close a list");
        literal.done(LIST_LITERAL);
    }

    /*
     * mapLiteralEntry
     *   : expression ":" expression
     *   ;
     */
    private void parseMapLiteralEntry() {
        PsiBuilder.Marker entry = mark();
        parseExpression();
        expect(COLON, "Expecting ':'");
        parseExpression();
        entry.done(MAP_LITERAL_ENTRY);
    }

    /*
     * "(" expression ")" // see tupleLiteral
     * "(" expression{","} ")"
     * TODO: Labels in tuple literals?
     */
    private void parseParenthesizedExpressionOrTuple() {
        assert at(LPAR);

        PsiBuilder.Marker mark = mark();

        advance(); // LPAR

        while (true) {
            if (at(COMMA)) errorAndAdvance("Expecting a tuple entry (expression)");
            parseExpression();
            if (!at(COMMA)) break;
            advance(); // COMMA
        }

        expect(RPAR, "Expecting ')'");

        mark.done(TUPLE);
    }

    /*
     * "this" ("<" type ">")?
     */
    private void parseThisExpression() {
        assert at(THIS_KEYWORD);
        advance(); // THIS_KEYWORD
        if (at(LT)) {
            // This may be "this < foo" or "this<foo>", thus the backtracking
            PsiBuilder.Marker supertype = mark();

            advance(); // LT

            myJetParsing.parseTypeRef();

            if (at(GT)) {
                advance(); // GT
                supertype.done(SUPERTYE_QUALIFIER);
            }
            else {
                supertype.rollbackTo();
            }
        }
    }

    /*
     * valueArguments
     *   : "(" (SimpleName "=")? ("out" | "ref")? expression{","} ")"
     *   ;
     */
    public void parseValueArgumentList() {
        PsiBuilder.Marker list = mark();

        expect(LPAR, "Expecting a parameter list", TokenSet.create(RPAR));

        if (!at(RPAR)) {
            while (true) {
                parseValueArgument();
                if (!at(COMMA)) break;
                advance(); // COMMA
            }
        }

        expect(RPAR, "Expecting ')'");

        list.done(VALUE_ARGUMENT_LIST);
    }

    /*
     * (SimpleName "=")? ("out" | "ref")? expression
     */
    private void parseValueArgument() {
        PsiBuilder.Marker argument = mark();
        JetNodeType type = VALUE_ARGUMENT;
        if (at(IDENTIFIER) && lookahead(1) == EQ) {
            advance(); // IDENTIFIER
            advance(); // EQ
            type = NAMED_ARGUMENT;
        }
        if (at(OUT_KEYWORD) || at(REF_KEYWORD)) advance(); // REF or OUT
        parseExpression();
        argument.done(type);
    }

    /*
     * objectLiteral
     *   : "object" delegationSpecifier{","}? classBody // Cannot make class body optional: foo(object F, a)
     *   ;
     */
    public void parseObjectLiteral() {
        assert at(OBJECT_KEYWORD);

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
    }

    private void parseOneTokenExpression(JetNodeType type) {
        PsiBuilder.Marker mark = mark();
        advance();
        mark.done(type);
    }

}
