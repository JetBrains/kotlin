package org.jetbrains.jet.lang.resolve.constants;

import com.google.common.base.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationArgumentVisitor;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.jetbrains.jet.lang.types.JetType;

/**
 * @author abreslav
 */
public class IntValue implements CompileTimeConstant<Integer> {
    public static final Function<Long, IntValue> CREATE = new Function<Long, IntValue>() {
        @Override
        public IntValue apply(@Nullable Long input) {
            assert input != null;
            return new IntValue(input.intValue());
        }
    };

    private final int value;

    public IntValue(int value) {
        this.value = value;
    }

    @Override
    public Integer getValue() {
        return value;
    }

    @NotNull
    @Override
    public JetType getType(@NotNull JetStandardLibrary standardLibrary) {
        return standardLibrary.getIntType();
    }

    @Override
    public <R, D> R accept(AnnotationArgumentVisitor<R, D> visitor, D data) {
        return visitor.visitIntValue(this, data);
    }

    @Override
    public String toString() {
        return value + ".int";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IntValue intValue = (IntValue) o;

        if (value != intValue.value) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return value;
    }
}
