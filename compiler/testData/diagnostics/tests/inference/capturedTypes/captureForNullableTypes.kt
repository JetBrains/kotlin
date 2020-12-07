// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// !CHECK_TYPE

fun <T: Any> bar(a: Array<T>): Array<T?> =  null!!

fun test1(a: Array<out Int>) {
    val r: Array<out Int?> = <!TYPE_MISMATCH{NI}!><!UNSUPPORTED{NI}!>bar<!>(a)<!>
    val t = <!UNSUPPORTED{NI}!>bar<!>(a)
    <!UNSUPPORTED{NI}!>t<!> checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER{NI}!>_<!><Array<out Int?>>() }
}

fun <T: Any> foo(l: Array<T>): Array<Array<T?>> = null!!

fun test2(a: Array<out Int>) {
    val r: Array<out Array<out Int?>> = <!TYPE_MISMATCH{NI}!><!UNSUPPORTED{NI}!>foo<!>(a)<!>
    val t = <!UNSUPPORTED{NI}!>foo<!>(a)
    <!UNSUPPORTED{NI}!>t<!> checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER{NI}!>_<!><Array<out Array<out Int?>>>() }
}
