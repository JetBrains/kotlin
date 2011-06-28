package org.jetbrains.jet.grammar;

/**
 * @author abreslav
 */
public class Comment extends Token {
    public Comment(CharSequence text) {
        super(text);
    }

    @Override
    public String toString() {
        return "";//getText().toString().replaceAll("\\{", "\\\\{").replaceAll("\\[", "\\\\[").replaceAll("\\(", "\\\\(");
    }
}
