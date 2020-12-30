// JAVA_SOURCES: WildcardsWithDefault.java

fun main(
            aNotNullNotNullNotNull: A<Any, Any, Any>,
            aNotNullNotNullNull: A<Any, Any, Any?>,
            aNotNullNullNotNull: A<Any, Any?, Any>,
            aNotNullNullNull: A<Any, Any?, Any?>,
            b: WildcardsWithDefault
): Unit {
    // jspecify_nullness_mismatch
    b.noBoundsNotNull(aNotNullNotNullNotNull)
    b.noBoundsNotNull(aNotNullNotNullNull)
    // jspecify_nullness_mismatch
    b.noBoundsNotNull(aNotNullNullNotNull)
    // jspecify_nullness_mismatch
    b.noBoundsNotNull(aNotNullNullNull)

    b.noBoundsNullable(aNotNullNotNullNotNull)
    b.noBoundsNullable(aNotNullNotNullNull)
    b.noBoundsNullable(aNotNullNullNotNull)
    b.noBoundsNullable(aNotNullNullNull)
}