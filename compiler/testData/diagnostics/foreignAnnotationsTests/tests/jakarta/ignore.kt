// FIR_IDENTICAL
// NULLABILITY_ANNOTATIONS: @jakarta.annotation:ignore

// FILE: A.java

import jakarta.annotation.*;

public class A<T> {
    @Nullable public String field = null;

    @Nullable
    public String foo(@Nonnull String x, @Nullable CharSequence y) {
        return "";
    }

    @Nonnull
    public String bar() {
        return "";
    }

    @Nullable
    public T baz(@Nonnull T x) { return x; }
}

// FILE: main.kt

fun main(a: A<String>, a1: A<String?>) {
    a.foo("", null)?.length
    a.foo("", null).length
    a.foo(null, "").length

    a.bar().length
    a.bar()!!.length

    a.field?.length
    a.field.length

    a.baz("").length
    a.baz("")?.length
    a.baz(null).length

    a1.baz("")!!.length
    a1.baz(null)!!.length
}