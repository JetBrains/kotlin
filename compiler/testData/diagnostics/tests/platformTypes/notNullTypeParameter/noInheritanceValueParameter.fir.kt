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
    A.create().<!INAPPLICABLE_CANDIDATE!>bar<!>(null)
    A.create().bar("")

    A<String>().<!INAPPLICABLE_CANDIDATE!>bar<!>(null)
    A<String?>().<!INAPPLICABLE_CANDIDATE!>bar<!>(null)
    A<String?>().bar("")
}
