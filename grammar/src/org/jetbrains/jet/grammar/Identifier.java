package org.jetbrains.jet.grammar;

/**
 * @author abreslav
 */
public class Identifier extends Token {
    private final String name;

    public Identifier(CharSequence text) {
        super(text);
        name = text.toString();
    }

    @Override
    public String toString() {
        return "[#" + getText() + "]";
    }

    public String getName() {
        return name;
    }
}
