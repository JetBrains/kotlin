package org.jetbrains.jet.lang.resolve.constants;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationArgumentVisitor;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.jetbrains.jet.lang.types.JetType;

/**
 * @author alex.tkachman
 */
public class FloatValue implements CompileTimeConstant<Float> {
    private final float value;

    public FloatValue(float value) {
        this.value = value;
    }

    @Override
    public Float getValue() {
        return value;
    }

    @NotNull
    @Override
    public JetType getType(@NotNull JetStandardLibrary standardLibrary) {
        return standardLibrary.getFloatType();
    }

    @Override
    public <R, D> R accept(AnnotationArgumentVisitor<R, D> visitor, D data) {
        return visitor.visitFloatValue(this, data);
    }

    @Override
    public String toString() {
        return value + ".flt";
    }
}
