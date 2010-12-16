package org.jetbrains.jet.lang.parsing;

import com.intellij.lang.PsiBuilder;
import org.jetbrains.jet.JetNodeType;

import static org.jetbrains.jet.JetNodeTypes.NAMED_ARGUMENT;
import static org.jetbrains.jet.JetNodeTypes.VALUE_ARGUMENT;
import static org.jetbrains.jet.JetNodeTypes.VALUE_ARGUMENT_LIST;
import static org.jetbrains.jet.lexer.JetTokens.*;

/**
 * @author abreslav
 */
public class JetExpressionParsing extends AbstractJetParsing {
    public JetExpressionParsing(SemanticWitespaceAwarePsiBuilder builder) {
        super(builder);
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
        advance(); // TODO
    }

    /*
     * valueArguments
     *   : "(" (SimpleName "=")? ("out" | "ref")? expression{","} ")"
     *   ;
     */
    public void parseValueArgumentList() {
        assert at(LPAR);

        PsiBuilder.Marker list = mark();

        advance(); // LPAR

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



}
