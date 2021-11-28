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
