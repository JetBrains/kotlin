// !WITH_NEW_INFERENCE

fun test1() {
    <!OI;TYPE_MISMATCH!>1<!>. <!NI;FUNCTION_EXPECTED!>(fun String.(i: Int) = i )<!>(1)
    <!OI;TYPE_MISMATCH!>1<!>.<!NI;FUNCTION_EXPECTED!>(label@ fun String.(i: Int) = i )<!>(1)
}

fun test2(f: String.(Int) -> Unit) {
    <!OI;TYPE_MISMATCH!>11<!>.<!NI;FUNCTION_EXPECTED!>(f)<!>(1)
    <!OI;TYPE_MISMATCH!>11<!>.<!NI;FUNCTION_EXPECTED!>(f)<!>(<!OI;NO_VALUE_FOR_PARAMETER!>)<!>
}

fun test3() {
    fun foo(): String.(Int) -> Unit = {}

    <!OI;TYPE_MISMATCH!>1<!>.<!NI;FUNCTION_EXPECTED!>(foo())<!>(1)
}