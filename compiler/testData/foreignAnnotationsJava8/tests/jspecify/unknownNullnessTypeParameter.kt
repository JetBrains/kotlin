// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// FILE: A.java

import jspecify.annotations.*;

public class A<T> {
    public void foo(T t) {}

    @DefaultNotNull
    public void bar(String s, T t) {} // t should not become not nullable
}

// FILE: main.kt

fun main(a1: A<Int>, a2: A<Int?>) {
    a1.foo(null)
    a1.foo(1)

    a2.foo(null)
    a2.foo(1)

    a1.bar(<!NULL_FOR_NONNULL_TYPE!>null<!>, null)
    a1.bar("", null)
    a1.bar("", 1)

    a2.bar(<!NULL_FOR_NONNULL_TYPE!>null<!>, null)
    a2.bar("", null)
    a2.bar("", 1)
}
