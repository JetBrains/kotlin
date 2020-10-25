// JAVA_SOURCES: Defaults.java
// JSPECIFY_STATE strict

fun main(a: Defaults, x: Foo): Unit {
    // jspecify_nullness_mismatch
    a.everythingNotNullable(<!NULL_FOR_NONNULL_TYPE!>null<!>).foo()
    a.everythingNotNullable(x).foo()

    a.everythingNullable(null)<!UNSAFE_CALL!>.<!>foo()

    a.everythingUnknown(null).foo()

    // jspecify_nullness_mismatch
    a.mixed(<!NULL_FOR_NONNULL_TYPE!>null<!>)<!UNSAFE_CALL!>.<!>foo()
    a.mixed(x)<!UNSAFE_CALL!>.<!>foo()

    a.explicitlyNullnessUnspecified(x).foo()
    a.explicitlyNullnessUnspecified(null).foo()

    a.defaultField.foo()

    a.field<!UNSAFE_CALL!>.<!>foo()
}