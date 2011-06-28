package org.jetbrains.jet.grammar;

/**
 * @author abreslav
 */
public class Token {
    private final CharSequence text;

    public Token(CharSequence text) {
        this.text = text;
    }

    public CharSequence getText() {
        return text;
    }


    @Override
    public String toString() {
        return getText().toString();
    }
}
