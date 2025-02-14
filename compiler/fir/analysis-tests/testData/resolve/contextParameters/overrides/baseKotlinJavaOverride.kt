// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-66195
// LANGUAGE: +ContextParameters

// FILE: JavaWithOverride.java
public class JavaWithOverride implements KotlinContextInterface {
    @Override
    public String foo(String a, int b) {
        return a + b;
    }

    @Override
    public double getBar(String a) {
        return "3.14";
    }

    @Override
    public double getBaz(String a, int b) {
        return "2.72";
    }
}

// FILE: JavaWithFakeOverride.java
public interface JavaWithFakeOverride implements KotlinContextInterface { }

// FILE: test.kt
interface KotlinContextInterface {
    context(a: String)
    fun foo(b: Int): String

    context(a: String)
    val bar: Double

    context(a: String)
    val Int.baz: Double
}

fun usage(a: JavaWithOverride, b: JavaWithFakeOverride) {
    a.<!NO_CONTEXT_ARGUMENT!>foo<!>(1)
    b.<!NO_CONTEXT_ARGUMENT!>foo<!>(1)
    a.<!NO_CONTEXT_ARGUMENT!>bar<!>
    b.<!NO_CONTEXT_ARGUMENT!>bar<!>

    a.<!NO_CONTEXT_ARGUMENT!>foo<!>(<!ARGUMENT_TYPE_MISMATCH!>""<!>, <!TOO_MANY_ARGUMENTS!>1<!>)
    b.<!NO_CONTEXT_ARGUMENT!>foo<!>(1, <!TOO_MANY_ARGUMENTS!>""<!>)
    a.<!UNRESOLVED_REFERENCE!>getBar<!>("")
    b.<!UNRESOLVED_REFERENCE!>getBar<!>("")
    a.<!UNRESOLVED_REFERENCE!>getBaz<!>("", 1)
    b.<!UNRESOLVED_REFERENCE!>getBaz<!>("", 1)

    with("OK") {
        a.bar
        b.bar
        a.<!UNRESOLVED_REFERENCE!>baz<!>
        b.<!UNRESOLVED_REFERENCE!>baz<!>
        <!CANNOT_INFER_PARAMETER_TYPE!>with<!>(4) {
            a.<!UNRESOLVED_REFERENCE!>baz<!>
            b.<!UNRESOLVED_REFERENCE!>baz<!>
        }
        with(a) { 4.baz }
        with(b) { 4.baz }
        a.foo(1)
        b.foo(1)
    }

    with("OK") {
        with(4) {
            with(a) { baz }
            with(b) { baz }
        }
    }
}
