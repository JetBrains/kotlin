package org.jetbrains.jet.lang.parsing;

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

}
