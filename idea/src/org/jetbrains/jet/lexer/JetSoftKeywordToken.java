/*
 * @author max
 */
package org.jetbrains.jet.lexer;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class JetSoftKeywordToken extends JetToken {
    private final String myValue;

    public JetSoftKeywordToken(@NotNull @NonNls String value) {
        super(value);
        myValue = value;
    }

    public String getValue() {
        return myValue;
    }
}
