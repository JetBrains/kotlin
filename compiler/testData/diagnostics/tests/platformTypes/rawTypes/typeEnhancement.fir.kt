// FILE: A.java

import org.jetbrains.annotations.*;
import java.util.*;

class A<T> {
    @NotNull
    List<String> foo(@NotNull T x, @Nullable List<String> y) {}
}

// FILE: Test.java

class Test {
    static class DerivedRawA extends A {}

    static A rawField = null;
}

// FILE: main.kt

val doubleList: List<Double?> = null!!

fun main() {
    Test.rawField.<!INAPPLICABLE_CANDIDATE!>foo<!>("", doubleList)
    Test.rawField.<!INAPPLICABLE_CANDIDATE!>foo<!>(null, doubleList)
    Test.DerivedRawA().<!INAPPLICABLE_CANDIDATE!>foo<!>(null, doubleList)
}
