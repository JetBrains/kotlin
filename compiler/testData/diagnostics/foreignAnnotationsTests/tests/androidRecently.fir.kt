// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER

// FILE: A.java
import androidx.annotation.*;

public class A<T> {
    @RecentlyNullable public String field = null;

    @RecentlyNullable
    public String foo(@RecentlyNonNull String x, @RecentlyNullable CharSequence y) {
        return "";
    }

    @RecentlyNonNull
    public String bar() {
        return "";
    }

    @RecentlyNullable
    public T baz(@RecentlyNonNull T x) { return x; }

    @RecentlyNonNull
    public T baz2(@RecentlyNullable T x) { return x; }
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

    a.baz2("").length
    a.baz2("")?.length
    a.baz2("")!!.length

    a.baz2(null).length
    a.baz2(null)?.length
    a.baz2(null)!!.length
}
