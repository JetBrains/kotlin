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
public class ShortValue implements CompileTimeConstant<Short> {
    public static final Function<Long, ShortValue> CREATE = new Function<Long, ShortValue>() {
        @Override
        public ShortValue apply(@Nullable Long input) {
            assert input != null;
            return new ShortValue(input.shortValue());
        }
    };

    private final short value;

    public ShortValue(short value) {
        this.value = value;
    }

    @Override
    public Short getValue() {
        return value;
    }

    @NotNull
    @Override
    public JetType getType(@NotNull JetStandardLibrary standardLibrary) {
        return standardLibrary.getShortType();
    }

    @Override
    public <R, D> R accept(AnnotationArgumentVisitor<R, D> visitor, D data) {
        return visitor.visitShortValue(this, data);
    }

    @Override
    public String toString() {
        return value + ".sht";
    }

}
