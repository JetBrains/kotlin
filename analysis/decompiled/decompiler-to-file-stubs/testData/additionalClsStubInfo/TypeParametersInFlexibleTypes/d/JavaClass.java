package d;

import org.jetbrains.annotations.*;
import kotlin.annotations.jvm.*;

public abstract class JavaClass<T> {
    @Nullable
    public abstract T foo();

    @Mutable
    public abstract java.util.Collection<T> bar();

    @Mutable
    public static <K> java.util.Collection<K> baz(K t) {
        return new java.util.ArrayList<>();
    }
}