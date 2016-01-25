// FILE: A.java

import java.util.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class A<T> {
    public static A<String> create() {
        return null;
    }

    public void bar(@NotNull T x) {
    }
}

// FILE: k.kt

fun test() {
    A.create().bar(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    A.create().bar("")

    A<String>().bar(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    A<String?>().bar(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    A<String?>().bar("")
}
