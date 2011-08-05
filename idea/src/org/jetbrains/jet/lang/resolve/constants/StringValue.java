package org.jetbrains.jet.lang.resolve.constants;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationArgumentVisitor;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.jetbrains.jet.lang.types.JetType;

/**
 * @author abreslav
 */
public class StringValue implements CompileTimeConstant<String> {

    private final String value;

    public StringValue(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }

    @NotNull
    @Override
    public JetType getType(@NotNull JetStandardLibrary standardLibrary) {
        return standardLibrary.getStringType();
    }

    @Override
    public <R, D> R accept(AnnotationArgumentVisitor<R, D> visitor, D data) {
        return visitor.visitStringValue(this, data);
    }

    @Override
    public String toString() {
        return "\"" + value + "\"";
    }
}
