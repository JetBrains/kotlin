package d;

import org.jetbrains.annotations.*;
import kotlin.annotations.jvm.*;

public abstract class JavaClass {
    @Nullable
    public abstract Integer foo();

    @Mutable
    public abstract java.util.Collection<Integer> bar();

    @Mutable
    public abstract java.util.Collection<java.util.Collection<Integer>> baz();
}