// FIR_DUMP

fun test1() {
    1. <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>(fun String.(i: Int) = i )<!>(1)
    1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>(label@ fun String.(i: Int) = i )<!>(1)
}

fun test2(f: String.(Int) -> Unit) {
    11.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>(f)<!>(1)
    11.(f)<!NO_VALUE_FOR_PARAMETER!>()<!>
}

fun test3() {
    fun foo(): String.(Int) -> Unit = {}

    1.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>(foo())<!>(1)
}
