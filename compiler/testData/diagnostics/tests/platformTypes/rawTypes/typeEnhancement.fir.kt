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
    Test.rawField.foo("", <!ARGUMENT_TYPE_MISMATCH!>doubleList<!>)
    Test.rawField.foo(<!ARGUMENT_TYPE_MISMATCH!>null<!>, <!ARGUMENT_TYPE_MISMATCH!>doubleList<!>)
    Test.DerivedRawA().foo(<!ARGUMENT_TYPE_MISMATCH!>null<!>, <!ARGUMENT_TYPE_MISMATCH!>doubleList<!>)
}
