// JAVA_SOURCES: WildcardsWithDefault.java
// JSPECIFY_STATE strict

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
    b.noBoundsNotNull(<!TYPE_MISMATCH!>aNotNullNullNotNull<!>)
    // jspecify_nullness_mismatch
    b.noBoundsNotNull(<!TYPE_MISMATCH!>aNotNullNullNull<!>)

    b.noBoundsNullable(aNotNullNotNullNotNull)
    b.noBoundsNullable(aNotNullNotNullNull)
    b.noBoundsNullable(aNotNullNullNotNull)
    b.noBoundsNullable(aNotNullNullNull)
}