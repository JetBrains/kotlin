//!DIAGNOSTICS: -UNUSED_PARAMETER

//FILE:Foo.java

public class Foo {
    public static String foo() {
        return null;
    }
}

//FILE:Bar.kt

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun <@kotlin.internal.OnlyInputTypes T> assertEquals1(t1: T, t2: T) {}

fun test() {
    assertEquals1(null, Foo.foo())
    assertEquals1("", Foo.foo())
}
