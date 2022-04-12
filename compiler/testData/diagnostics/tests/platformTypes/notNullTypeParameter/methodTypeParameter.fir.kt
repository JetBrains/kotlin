// FILE: A.java

import org.jetbrains.annotations.NotNull;

public class A {
    public static <T> void bar(@NotNull T x, T y) { }
    public static String platformString() { return null; }
}

// FILE: k.kt

fun test() {
    A.bar(null, "")

    A.bar<String>(<!NULL_FOR_NONNULL_TYPE!>null<!>, "")
    A.bar<String?>(null, "")
    A.bar(null, A.platformString())
}
