// JAVA_SOURCES: TypeArgumentsFromParameterBounds.java
// JSPECIFY_STATE strict

fun main(
            aNotNullNotNullNotNull: TypeArgumentsFromParameterBounds<Test, Test, Test>,
            aNotNullNotNullNull: TypeArgumentsFromParameterBounds<Test, Test, Test?>,
            aNotNullNullNotNull: TypeArgumentsFromParameterBounds<Test, Test?, Test>,
            aNotNullNullNull: TypeArgumentsFromParameterBounds<Test, Test?, Test?>,
            a: A, b: B
): Unit {
    a.bar(aNotNullNotNullNotNull)
    // jspecify_nullness_mismatch
    a.bar(<!TYPE_MISMATCH!>aNotNullNotNullNull<!>)
    // jspecify_nullness_mismatch
    a.bar(<!TYPE_MISMATCH!>aNotNullNullNotNull<!>)
    // jspecify_nullness_mismatch
    a.bar(<!TYPE_MISMATCH!>aNotNullNullNull<!>)

    // jspecify_nullness_mismatch
    b.bar(<!TYPE_MISMATCH!>aNotNullNotNullNotNull<!>)
    // jspecify_nullness_mismatch
    b.bar(<!TYPE_MISMATCH!>aNotNullNotNullNull<!>)
    b.bar(aNotNullNullNotNull)
    b.bar(aNotNullNullNull)
}