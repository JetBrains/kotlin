package org.jetbrains.jet.lang.resolve.constants;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationArgumentVisitor;
import org.jetbrains.jet.lang.types.JetStandardClasses;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.jetbrains.jet.lang.types.JetType;

/**
 * @author abreslav
 */
public class NullValue implements CompileTimeConstant<Void> {

    public static final NullValue NULL = new NullValue();

    private NullValue() {
    }

    @Override
    public Void getValue() {
        return null;
    }

    @NotNull
    @Override
    public JetType getType(@NotNull JetStandardLibrary standardLibrary) {
        return JetStandardClasses.getNullableNothingType();
    }

    @Override
    public <R, D> R accept(AnnotationArgumentVisitor<R, D> visitor, D data) {
        return visitor.visitNullValue(this, data);
    }

    @Override
    public String toString() {
        return "null";
    }
}
