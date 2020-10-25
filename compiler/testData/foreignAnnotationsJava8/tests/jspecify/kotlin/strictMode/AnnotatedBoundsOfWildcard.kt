// JAVA_SOURCES: AnnotatedBoundsOfWildcard.java
// JSPECIFY_STATE strict

fun main(
            aNotNullNotNullNotNull: Test<Derived, Derived, Derived>,
            aNotNullNotNullNull: Test<Derived, Derived, Derived?>,
            aNotNullNullNotNull: Test<Derived, Derived?, Derived>,
            aNotNullNullNull: Test<Derived, Derived?, Derived?>,

            aAnyNotNullNotNullNotNull: Test<Any, Any, Any>,
            aAnyNotNullNotNullNull: Test<Any, Any, Any?>,
            aAnyNotNullNullNotNull: Test<Any, Any?, Any>,
            aAnyNotNullNullNull: Test<Any, Any?, Any?>,

            b: AnnotatedBoundsOfWildcard
): Unit {
    // jspecify_nullness_mismatch
    b.superAsIs(aAnyNotNullNotNullNotNull)
    // jspecify_nullness_mismatch
    b.superAsIs(aAnyNotNullNotNullNull)
    b.superAsIs(aAnyNotNullNullNotNull)
    b.superAsIs(aAnyNotNullNullNull)

    b.superNotNull(aAnyNotNullNotNullNotNull)
    b.superNotNull(aAnyNotNullNotNullNull)
    b.superNotNull(aAnyNotNullNullNotNull)
    b.superNotNull(aAnyNotNullNullNull)

    // jspecify_nullness_mismatch
    b.superNullable(<!TYPE_MISMATCH!>aAnyNotNullNotNullNotNull<!>)
    // jspecify_nullness_mismatch
    b.superNullable(<!TYPE_MISMATCH!>aAnyNotNullNotNullNull<!>)
    // jspecify_nullness_mismatch
    b.superNullable(<!TYPE_MISMATCH!>aAnyNotNullNullNotNull<!>)
    // jspecify_nullness_mismatch
    b.superNullable(<!TYPE_MISMATCH!>aAnyNotNullNullNull<!>)

    b.extendsAsIs(aNotNullNotNullNotNull)
    b.extendsAsIs(aNotNullNotNullNull)
    b.extendsAsIs(aNotNullNullNotNull)
    b.extendsAsIs(aNotNullNullNull)

    b.extendsNotNull(aNotNullNotNullNotNull)
    // jspecify_nullness_mismatch
    b.extendsNotNull(<!TYPE_MISMATCH!>aNotNullNotNullNull<!>)
    // jspecify_nullness_mismatch
    b.extendsNotNull(<!TYPE_MISMATCH!>aNotNullNullNotNull<!>)
    // jspecify_nullness_mismatch
    b.extendsNotNull(<!TYPE_MISMATCH!>aNotNullNullNull<!>)

    b.extendsNullable(aNotNullNotNullNotNull)
    b.extendsNullable(aNotNullNotNullNull)
    b.extendsNullable(aNotNullNullNotNull)
    b.extendsNullable(aNotNullNullNull)

    b.noBounds(aNotNullNotNullNotNull)
    b.noBounds(<!TYPE_MISMATCH!>aNotNullNotNullNull<!>)
    b.noBounds(<!TYPE_MISMATCH!>aNotNullNullNotNull<!>)
    b.noBounds(<!TYPE_MISMATCH!>aNotNullNullNull<!>)
}