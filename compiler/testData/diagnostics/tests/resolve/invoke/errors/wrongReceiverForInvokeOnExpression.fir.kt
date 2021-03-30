// !WITH_NEW_INFERENCE

fun test1() {
    1. <!UNRESOLVED_REFERENCE!>(fun String.(i: Int) = i )<!>(1)
    1.<!UNRESOLVED_REFERENCE!>(label@ fun String.(i: Int) = i )<!>(1)
}

fun test2(f: String.(Int) -> Unit) {
    <!ARGUMENT_TYPE_MISMATCH!>11<!>.(f)(1)
    11.<!INAPPLICABLE_CANDIDATE!>(f)<!>()
}

fun test3() {
    fun foo(): String.(Int) -> Unit = {}

    1.<!UNRESOLVED_REFERENCE!>(foo())<!>(1)
}
