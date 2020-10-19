// JAVA_SOURCES: Simple.java
// JSPECIFY_STATE strict

fun main(a: Simple, x: Derived): Unit {
    a.foo(x, null)<!UNSAFE_CALL!>.<!>foo()
    // jspecify_nullness_mismatch
    a.foo(<!NULL_FOR_NONNULL_TYPE!>null<!>, x)<!UNSAFE_CALL!>.<!>foo()

    a.bar().foo()

    a.field<!UNSAFE_CALL!>.<!>foo()
}