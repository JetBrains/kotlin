// FIR_IDENTICAL
// ISSUE: KT-63596

fun e(block: () -> String): String = ""
fun e(block: () -> Unit): Int = 0
fun c(block: (x: Int) -> String): String = ""
fun c(block: (x: Int) -> Unit): Int = 0

fun test() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>e<!> {}
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>c<!> {}
}
