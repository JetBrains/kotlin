fun <T> IntRange.flatMap(<!UNUSED_PARAMETER!>f<!>: (Int) -> List<T>): List<T> = throw AssertionError("")
fun <T, R> Iterable<T>.flatMap(<!UNUSED_PARAMETER!>f<!>: (T) -> List<R>): List<R> = throw AssertionError("")

fun foo(): List<Int> = for (<!CANNOT_INFER_PARAMETER_TYPE!>i<!> in <!COMPREHENSION_FLAT_MAP_AMBIGUITY!>1..2<!>, <!CANNOT_INFER_PARAMETER_TYPE!>j<!> in 1..<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>i<!>) yield <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>i<!><!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>*<!><!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>j<!>