// JAVA_SOURCES: TypeArgumentsFromParameterBounds.java

fun main(
            aNotNullNotNullNotNull: TypeArgumentsFromParameterBounds<Test, Test, Test>,
            aNotNullNotNullNull: TypeArgumentsFromParameterBounds<Test, Test, Test?>,
            aNotNullNullNotNull: TypeArgumentsFromParameterBounds<Test, Test?, Test>,
            aNotNullNullNull: TypeArgumentsFromParameterBounds<Test, Test?, Test?>,
            a: A, b: B
): Unit {
    a.bar(aNotNullNotNullNotNull)
    // jspecify_nullness_mismatch
    a.bar(aNotNullNotNullNull)
    // jspecify_nullness_mismatch
    a.bar(aNotNullNullNotNull)
    // jspecify_nullness_mismatch
    a.bar(aNotNullNullNull)

    // jspecify_nullness_mismatch
    b.bar(aNotNullNotNullNotNull)
    // jspecify_nullness_mismatch
    b.bar(aNotNullNotNullNull)
    b.bar(aNotNullNullNotNull)
    b.bar(aNotNullNullNull)
}