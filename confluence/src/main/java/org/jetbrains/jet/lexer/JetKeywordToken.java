/*
 * @author max
 */
package org.jetbrains.jet.lexer;


public class JetKeywordToken extends JetToken {

    public static JetKeywordToken keyword(String value) {
        return new JetKeywordToken(value, false);
    }

    public static JetKeywordToken softKeyword(String value) {
        return new JetKeywordToken(value, true);
    }

    private final String myValue;
    private final boolean myIsSoft;

    private JetKeywordToken(String value, boolean isSoft) {
        super(value);
        myValue = value;
        myIsSoft = isSoft;
    }

    public String getValue() {
        return myValue;
    }

    public boolean isSoft() {
        return myIsSoft;
    }
}
