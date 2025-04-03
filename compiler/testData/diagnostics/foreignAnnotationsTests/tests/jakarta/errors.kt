// FIR_IDENTICAL
// NULLABILITY_ANNOTATIONS: @jakarta.annotation:strict

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
    a.foo("", null)<!UNSAFE_CALL!>.<!>length
    a.foo(<!NULL_FOR_NONNULL_TYPE!>null<!>, "")<!UNSAFE_CALL!>.<!>length

    a.bar().length
    a.bar()<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>.length

    a.field?.length
    a.field<!UNSAFE_CALL!>.<!>length

    a.baz("")<!UNSAFE_CALL!>.<!>length
    a.baz("")?.length
    a.baz(<!NULL_FOR_NONNULL_TYPE!>null<!>)<!UNSAFE_CALL!>.<!>length

    a1.baz("")!!.length
    a1.baz(<!NULL_FOR_NONNULL_TYPE!>null<!>)!!.length
}
