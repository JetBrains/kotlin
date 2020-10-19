// JAVA_SOURCES: IgnoreAnnotations.java
// JSPECIFY_STATE strict

fun main(a: IgnoreAnnotations, x: Derived): Unit {
    a.foo(x, null)<!UNSAFE_CALL!>.<!>foo()
    // jspecify_nullness_mismatch
    a.foo(<!NULL_FOR_NONNULL_TYPE!>null<!>, x)<!UNSAFE_CALL!>.<!>foo()

    a.field<!UNSAFE_CALL!>.<!>foo()

    // jspecify_nullness_mismatch
    a.everythingNotNullable(<!NULL_FOR_NONNULL_TYPE!>null<!>).foo()
    a.everythingNotNullable(x).foo()

    a.everythingNullable(null)<!UNSAFE_CALL!>.<!>foo()

    a.everythingUnknown(null).foo()
}