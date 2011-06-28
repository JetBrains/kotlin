package org.jetbrains.jet.grammar;

/**
 * @author abreslav
 */
public class SymbolToken extends Token {
    public SymbolToken(CharSequence text) {
        super(text);
    }

    @Override
    public String toString() {
        return "{color:blue}*" + getText().toString().replaceAll("\\*", "\\\\*") + "*{color}";
    }
}
