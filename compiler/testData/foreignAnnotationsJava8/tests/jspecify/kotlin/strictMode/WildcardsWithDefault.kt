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
    b.noBoundsNotNull(<!TYPE_MISMATCH!>aNotNullNotNullNull<!>)
    // jspecify_nullness_mismatch
    b.noBoundsNotNull(<!TYPE_MISMATCH!>aNotNullNullNotNull<!>)
    // jspecify_nullness_mismatch
    b.noBoundsNotNull(<!TYPE_MISMATCH!>aNotNullNullNull<!>)

    b.noBoundsNullable(aNotNullNotNullNotNull)
    b.noBoundsNullable(<!TYPE_MISMATCH!>aNotNullNotNullNull<!>)
    b.noBoundsNullable(<!TYPE_MISMATCH!>aNotNullNullNotNull<!>)
    b.noBoundsNullable(<!TYPE_MISMATCH!>aNotNullNullNull<!>)
}