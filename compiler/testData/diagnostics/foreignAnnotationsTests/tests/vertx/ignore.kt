// FIR_IDENTICAL
// NULLABILITY_ANNOTATIONS: @io.vertx.codegen.annotations:ignore

// FILE: A.java

import io.vertx.codegen.annotations.*;

public class A<T> {
    @Nullable public String field = null;

    @Nullable
    public String foo(String x, @Nullable CharSequence y) {
        return "";
    }

    @Nullable
    public T baz(T x) { return x; }
}

// FILE: main.kt

fun main(a: A<String>, a1: A<String?>) {
    a.foo("", null)?.length
    a.foo("", null).length
    a.foo(null, "").length

    a.field?.length
    a.field.length

    a.baz("").length
    a.baz("")?.length
    a.baz(null).length

    a1.baz("")!!.length
    a1.baz(null)!!.length
}