package org.jetbrains.jet.grammar;

/**
 * @author abreslav
 */
public class Declaration extends Token {
    private final String name;

    public Declaration(CharSequence text) {
        super(text);
        name = text.toString().substring(1);
    }

    @Override
    public String toString() {
        return "{anchor:" + name + "}*" + name + "*";
    }

    public String getName() {
        return name;
    }
}
