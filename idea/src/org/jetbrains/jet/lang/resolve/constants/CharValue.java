package org.jetbrains.jet.lang.resolve.constants;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationArgumentVisitor;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.jetbrains.jet.lang.types.JetType;

/**
 * @author abreslav
 */
public class CharValue implements CompileTimeConstant<Character> {

    private final char value;

    public CharValue(char value) {
        this.value = value;
    }

    @Override
    public Character getValue() {
        return value;
    }

    @NotNull
    @Override
    public JetType getType(@NotNull JetStandardLibrary standardLibrary) {
        return standardLibrary.getCharType();
    }

    @Override
    public <R, D> R accept(AnnotationArgumentVisitor<R, D> visitor, D data) {
        return visitor.visitCharValue(this, data);
    }

    @Override
    public String toString() {
        return "#" + ((int) value) + "(" + value + ")";
    }
}
