// FIR_IDENTICAL
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
    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>a.foo("", null)<!>.length
    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>a.foo(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>, "")<!>.length
    a.foo("", null)!!.length

    a.bar().length
    a.bar()<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>.length

    a.field?.length
    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>a.field<!>.length

    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>a.baz("")<!>.length
    a.baz("")?.length
    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>a.baz(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)<!>.length

    a1.baz("")!!.length
    a1.baz(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)!!.length
}
