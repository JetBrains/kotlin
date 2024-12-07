package d;

import org.jetbrains.annotations.*;
import kotlin.annotations.jvm.*;

public abstract class JavaClass<T> {
    public class InnerClass<M> {}

    @Mutable
    public static <K, L> JavaClass<K>.InnerClass<L> baz(K k, L l) {
        return null;
    }
}