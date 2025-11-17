// FIR_IDENTICAL
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
    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>a.foo("", null)<!>.length
    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>a.foo(null, "")<!>.length
    a.foo("", null)!!.length

    a.field?.length
    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>a.field<!>.length

    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>a.baz("")<!>.length
    a.baz("")?.length
    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>a.baz(null)<!>.length

    a1.baz("")!!.length
    a1.baz(null)!!.length
}
