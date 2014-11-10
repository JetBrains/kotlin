fun Int.map(<!UNUSED_PARAMETER!>f<!>: (Any) -> Number): List<Int> = throw AssertionError("")
fun Number.map(<!UNUSED_PARAMETER!>f<!>: (Number) -> Int): List<Int> = throw AssertionError("")

fun foo(): List<Int> = for (<!CANNOT_INFER_PARAMETER_TYPE!>i<!> in <!COMPREHENSION_MAP_AMBIGUITY!>10<!>) yield <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>i<!>