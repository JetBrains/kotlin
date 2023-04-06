package d;

import org.jetbrains.annotations.*;
import kotlin.annotations.jvm.*;

public abstract class JavaClass<T> {
    @NotNull
    @Nullable
    public abstract T foo();

    @Mutable
    @ReadOnly
    public abstract java.util.Collection<T> bar();
}