// NULLABILITY_ANNOTATIONS: @org.chromium.build.annotations:ignore

// FILE: A.java

import org.chromium.build.annotations.*;

public class A<T> {
    @Nullable public String field = null;

    @Nullable
    public String foo(String x, @Nullable CharSequence y) {
        return "";
    }

    @NonNull
    public String bar() {
        return "";
    }

    @Nullable
    public T baz(T x) { return x; }
}

// FILE: B.java

import org.chromium.build.annotations.*;

@NullMarked
public class B {
    public String produce() { return ""; }

    public void consume(String x) {}
}

// FILE: main.kt

fun main(a: A<String>, a1: A<String?>) {
    a.foo("", null)?.length
    a.foo("", null).length
    a.foo(null, "").length

    a.field?.length
    a.field.length

    a.bar().length

    a.baz("").length
    a.baz("")?.length
    a.baz(null).length

    a1.baz("")!!.length
    a1.baz(null)!!.length
}

fun nullMarked(b: B) {
    b.produce().length
    b.consume("")
    b.consume(null)
}
