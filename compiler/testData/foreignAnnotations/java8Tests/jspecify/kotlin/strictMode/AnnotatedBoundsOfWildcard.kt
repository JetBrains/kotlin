// JAVA_SOURCES: AnnotatedBoundsOfWildcard.java

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
    b.superNullable(aAnyNotNullNotNullNotNull)
    // jspecify_nullness_mismatch
    b.superNullable(aAnyNotNullNotNullNull)
    // jspecify_nullness_mismatch
    b.superNullable(aAnyNotNullNullNotNull)
    // jspecify_nullness_mismatch
    b.superNullable(aAnyNotNullNullNull)

    b.extendsAsIs(aNotNullNotNullNotNull)
    b.extendsAsIs(aNotNullNotNullNull)
    b.extendsAsIs(aNotNullNullNotNull)
    b.extendsAsIs(aNotNullNullNull)

    b.extendsNotNull(aNotNullNotNullNotNull)
    // jspecify_nullness_mismatch
    b.extendsNotNull(aNotNullNotNullNull)
    // jspecify_nullness_mismatch
    b.extendsNotNull(aNotNullNullNotNull)
    // jspecify_nullness_mismatch
    b.extendsNotNull(aNotNullNullNull)

    b.extendsNullable(aNotNullNotNullNotNull)
    b.extendsNullable(aNotNullNotNullNull)
    b.extendsNullable(aNotNullNullNotNull)
    b.extendsNullable(aNotNullNullNull)

    b.noBounds(aNotNullNotNullNotNull)
    b.noBounds(aNotNullNotNullNull)
    b.noBounds(aNotNullNullNotNull)
    b.noBounds(aNotNullNullNull)
}