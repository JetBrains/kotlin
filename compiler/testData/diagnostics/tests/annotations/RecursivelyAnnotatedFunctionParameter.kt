// Function parameter CAN be recursively annotated
annotation class ann(val x: Int)
fun foo(@ann(<!ANNOTATION_PARAMETER_MUST_BE_CONST!>foo(1)<!>) x: Int): Int = x