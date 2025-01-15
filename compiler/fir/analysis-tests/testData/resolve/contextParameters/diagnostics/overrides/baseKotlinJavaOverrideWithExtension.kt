// RUN_PIPELINE_TILL: FRONTEND
// TARGET_BACKEND: JVM
// LANGUAGE: +ContextParameters

// FILE: JavaWithOverride.java
public class JavaWithOverride implements KotlinContextInterface {
    @Override
    public String foo(String a, int b, boolean c) {
        return a + b + c;
    }
}

// FILE: JavaWithFakeOverride.java
public interface JavaWithFakeOverride implements KotlinContextInterface { }

// FILE: test.kt
interface KotlinContextInterface {
    context(a: String)
    fun Int.foo(b: Boolean): String
}

fun usage(a: JavaWithOverride, b: JavaWithFakeOverride) {
    a.foo("", 1, true)
    b.<!UNRESOLVED_REFERENCE!>foo<!>("", 1, true)
    with("OK") {
        with(a) {
            1.foo(true)
        }
        with(b) {
            1.foo(true)
        }
    }

    with(a) {
        1.<!NO_CONTEXT_ARGUMENT!>foo<!>(true)
    }
    with(b) {
        1.<!NO_CONTEXT_ARGUMENT!>foo<!>(true)
    }
}