// FILE: A.java

import java.util.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class A<T> {
    public static A<String> create() {
        return null;
    }

    @NotNull
    public T bar() {
    }
}

// FILE: k.kt

fun test() {
    A.create().bar()?.length
    A<String?>().bar()?.length
}
