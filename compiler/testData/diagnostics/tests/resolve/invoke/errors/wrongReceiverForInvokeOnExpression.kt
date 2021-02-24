// !WITH_NEW_INFERENCE

fun test1() {
    <!TYPE_MISMATCH{OI}!>1<!>. <!FUNCTION_EXPECTED{NI}!>(fun String.(i: Int) = i )<!>(1)
    <!TYPE_MISMATCH{OI}!>1<!>.<!FUNCTION_EXPECTED{NI}!>(label@ fun String.(i: Int) = i )<!>(1)
}

fun test2(f: String.(Int) -> Unit) {
    <!TYPE_MISMATCH{OI}!>11<!>.<!FUNCTION_EXPECTED{NI}!>(f)<!>(1)
    <!TYPE_MISMATCH{OI}!>11<!>.<!FUNCTION_EXPECTED{NI}!>(f)<!>(<!NO_VALUE_FOR_PARAMETER{OI}!>)<!>
}

fun test3() {
    fun foo(): String.(Int) -> Unit = {}

    <!TYPE_MISMATCH{OI}!>1<!>.<!FUNCTION_EXPECTED{NI}!>(foo())<!>(1)
}
