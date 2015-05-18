package d

import org.jetbrains.annotations.*;

public abstract class JavaClass {
    @NotNull
    @Nullable
    public abstract Integer foo();

    @Mutable
    @ReadOnly
    public abstract java.util.Collection<Integer> bar();
}