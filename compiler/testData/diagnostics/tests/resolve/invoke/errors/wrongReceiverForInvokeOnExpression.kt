// FIR_DUMP

fun test1() {
    1. <!FUNCTION_EXPECTED!>(fun String.(i: Int) = i )<!>(1)
    1.<!FUNCTION_EXPECTED!>(label@ fun String.(i: Int) = i )<!>(1)
}

fun test2(f: String.(Int) -> Unit) {
    11.<!FUNCTION_EXPECTED!>(f)<!>(1)
    11.<!FUNCTION_EXPECTED!>(f)<!>()
}

fun test3() {
    fun foo(): String.(Int) -> Unit = {}

    1.<!FUNCTION_EXPECTED!>(foo())<!>(1)
}
