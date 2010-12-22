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
     *   : memberAccessExpression
     *   : expressionWithPrecedences
     *   : match
     *   : if
     *   : "typeof"
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
            // TODO (expression) or tuple
        }
        else if (at(INTEGER_LITERAL)) {
            // TODO
        }
        else if (at(CHARACTER_LITERAL)) {
            // TODO
        }
        else if (at(FLOAT_LITERAL)) {
            // TODO
        }
        else if (at(STRING_LITERAL)) {
            // TODO
        }
        else if (at(TRUE_KEYWORD)) {
            // TODO
        }
        else if (at(FALSE_KEYWORD)) {
            // TODO
        }
        else if (at(LBRACKET)) {
            // TODO: map, list or range
        }
        else if (at(NULL_KEYWORD)) {
            // TODO
        }
        else if (at(THIS_KEYWORD)) {
            // TODO
        }
        else if (at(IF_KEYWORD)) {
            // TODO
        }
        else if (at(TYPEOF_KEYWORD)) {
            // TODO
        }
        else if (at(NEW_KEYWORD)) {
            // TODO
        }
        else if (at(OBJECT_KEYWORD)) {
            // TODO
        }
        else if (at(THROW_KEYWORD)) {
            // TODO
        }
        else if (at(RETURN_KEYWORD)) {
            // TODO
        }
        else if (at(CONTINUE_KEYWORD)) {
            // TODO
        }
        else if (at(BREAK_KEYWORD)) {
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
        else if (atSet(TokenSet.create(CLASS_KEYWORD, EXTENSION_KEYWORD, FUN_KEYWORD, VAL_KEYWORD, VAR_KEYWORD, TYPE_KEYWORD))) {
            // TODO
        }
        advance(); // TODO
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
     *   : "object" delegationSpecifier{","} classBody?
     *   : "object" classBody
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
}
