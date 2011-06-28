package org.jetbrains.jet.grammar;

/**
 * @author abreslav
 */
public class WhiteSpace extends Token {
    public WhiteSpace(CharSequence text) {
        super(text);
    }

    @Override
    public String toString() {
        return super.toString().replaceAll(" ", "&nbsp;");
    }
}
