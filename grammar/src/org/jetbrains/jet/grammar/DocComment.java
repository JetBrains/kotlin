package org.jetbrains.jet.grammar;

/**
 * @author abreslav
 */
public class DocComment extends Token {
    public DocComment(CharSequence text) {
        super(text);
    }

    @Override
    public String toString() {
        String s = super.toString();
        return s.substring(3, s.length() - 2);
    }
}
