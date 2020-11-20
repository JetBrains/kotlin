// JAVA_SOURCES: Defaults.java

fun main(a: Defaults, x: Foo): Unit {
    // jspecify_nullness_mismatch
    a.everythingNotNullable(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>).foo()
    a.everythingNotNullable(x).foo()

    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>a.everythingNullable(null)<!>.foo()

    a.everythingUnknown(null).foo()

    // jspecify_nullness_mismatch
    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>a.mixed(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>null<!>)<!>.foo()
    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>a.mixed(x)<!>.foo()

    a.explicitlyNullnessUnspecified(x).foo()
    a.explicitlyNullnessUnspecified(null).foo()

    a.defaultField.foo()

    <!RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>a.field<!>.foo()
}