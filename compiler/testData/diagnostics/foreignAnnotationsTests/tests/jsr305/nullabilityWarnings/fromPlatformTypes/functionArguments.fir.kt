// !DIAGNOSTICS: -UNUSED_PARAMETER
// LANGUAGE: +ContextReceivers
// JSR305_GLOBAL_REPORT: warn

// FILE: J.java
public class J {
    @MyNonnull
    public static J staticNN;
    @MyNullable
    public static J staticN;
    public static J staticJ;
}

// FILE: k.kt
fun test() {
    foo(J.staticNN)
    foo(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>J.staticN<!>)
    foo(J.staticJ)

    bar(J.staticNN)
    bar(J.staticN)
    bar(J.staticJ)

    with(J.staticNN) { baz() }
    with(J.staticN) { <!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>baz()<!> }
    with(J.staticJ) { baz() }

    with(J.staticNN) { qux() }
    with(J.staticN) { qux() }
    with(J.staticJ) { qux() }
}

fun foo(j: J) {}
fun bar(j: J?) {}
context(J) fun baz() {}
context(J?) fun qux() {}
