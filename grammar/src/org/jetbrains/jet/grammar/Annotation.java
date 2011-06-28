package org.jetbrains.jet.grammar;

/**
 * @author abreslav
 */
public class Annotation extends Token {
    public Annotation(CharSequence text) {
        super(text);
    }

    @Override
    public String toString() {
        return "{{" + super.toString().replaceAll("\\[", "").replaceAll("\\]", "") + "}}";
    }
}
