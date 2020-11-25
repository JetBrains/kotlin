package test;

import java.lang.annotation.*;

public final class ArrayTypeVariance {
    @Documented
    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.TYPE_USE)
    public @interface NotNull {}

    class Foo<T> {}

    public final Object[] toArray(Foo<@NotNull Integer> p0) {
        throw new UnsupportedOperationException();
    }
}
