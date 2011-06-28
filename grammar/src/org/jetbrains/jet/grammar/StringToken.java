package org.jetbrains.jet.grammar;

/**
 * @author abreslav
 */
public class StringToken extends Token {
    public StringToken(CharSequence text) {
        super(text);
    }

    @Override
    public String toString() {
        return "{color:green}*{{" +
               getText().toString()
                       .replaceAll("\\{", "\\\\{")
                       .replaceAll("\\[", "\\\\[")
                       .replaceAll("!", "\\\\!")
                       .replaceAll("\\*", "\\\\*")
               + "}}*{color}";
    }
}
