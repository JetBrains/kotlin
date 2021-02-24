// JAVA_SOURCES: TypeArgumentsFromParameterBounds.java
// JSPECIFY_STATE warn

fun main(
            aNotNullNotNullNotNull: TypeArgumentsFromParameterBounds<Test, Test, Test>,
            aNotNullNotNullNull: TypeArgumentsFromParameterBounds<Test, Test, Test?>,
            aNotNullNullNotNull: TypeArgumentsFromParameterBounds<Test, Test?, Test>,
            aNotNullNullNull: TypeArgumentsFromParameterBounds<Test, Test?, Test?>,
            a: A, b: B, c: C
): Unit {
    // jspecify_nullness_mismatch
    a.bar(aNotNullNotNullNotNull)
    // jspecify_nullness_mismatch
    a.bar(aNotNullNotNullNull)
    // jspecify_nullness_mismatch
    a.bar(aNotNullNullNotNull)
    a.bar(aNotNullNullNull)

    b.bar(aNotNullNotNullNotNull)
    // jspecify_nullness_mismatch
    b.bar(aNotNullNotNullNull)
    // jspecify_nullness_mismatch
    b.bar(aNotNullNullNotNull)
    // jspecify_nullness_mismatch
    b.bar(aNotNullNullNull)

    // jspecify_nullness_mismatch
    c.bar(aNotNullNotNullNotNull)
    // jspecify_nullness_mismatch
    c.bar(aNotNullNotNullNull)
    c.bar(aNotNullNullNotNull)
    c.bar(aNotNullNullNull)
}