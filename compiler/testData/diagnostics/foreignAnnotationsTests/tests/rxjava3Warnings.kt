// FIR_IDENTICAL
// NULLABILITY_ANNOTATIONS: @io.reactivex.rxjava3.annotations:warn

// FILE: A.java

import io.reactivex.rxjava3.annotations.*;

public class A<T> {
    @Nullable public String field = null;

    @Nullable
    public String foo(@NonNull String x, @Nullable CharSequence y) {
        return "";
    }

    @NonNull
    public String bar() {
        return "";
    }

    @Nullable
    public T baz(@NonNull T x) { return x; }
}

// FILE: main.kt

fun main(a: A<String>, a1: A<String?>) {
    a.foo("", null)?.length
    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>a.foo("", null)<!>.length
    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>a.foo(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>, "")<!>.length

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