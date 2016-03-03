// FILE: A.java

import org.jetbrains.annotations.*;
import java.util.*;

class A<T> {
    @NotNull
    List<@Nullable String> foo(@NotNull T x, @Nullable List<@NotNull String> y) {}
}

// FILE: Test.java

class Test {
    static class DerivedRawA extends A {}

    static A rawField = null;
}

// FILE: main.kt

val doubleList: List<Double?> = null!!

fun main() {
    Test.rawField.foo("", doubleList)
    Test.rawField.foo(<!NULL_FOR_NONNULL_TYPE!>null<!>, doubleList)
    Test.DerivedRawA().foo(<!NULL_FOR_NONNULL_TYPE!>null<!>, doubleList)
}
