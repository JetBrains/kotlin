// RUN_PIPELINE_TILL: FRONTEND
// TARGET_BACKEND: JVM
// LANGUAGE: +ContextParameters

// FILE: JavaWithOverride.java
public class JavaWithOverride implements KotlinContextInterface {
    @Override
    public String foo(String a, int b) {
        return a + b;
    }
}

// FILE: JavaWithFakeOverride.java
public interface JavaWithFakeOverride implements KotlinContextInterface { }

// FILE: test.kt
interface KotlinContextInterface {
    context(a: String)
    fun foo(b: Int): String
}

fun usage(a: JavaWithOverride, b: JavaWithFakeOverride) {
    a.<!NO_CONTEXT_ARGUMENT!>foo<!>(1)
    b.<!NO_CONTEXT_ARGUMENT!>foo<!>(1)

    a.foo("", 1)
    b.<!NO_CONTEXT_ARGUMENT!>foo<!>(1, <!TOO_MANY_ARGUMENTS!>""<!>)

    with("OK") {
        a.foo(1)
        b.foo(1)
    }
}