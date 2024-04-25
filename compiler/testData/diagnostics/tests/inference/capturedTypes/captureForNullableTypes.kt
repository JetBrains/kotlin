// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// CHECK_TYPE

fun <T: Any> bar(a: Array<T>): Array<T?> =  null!!

fun test1(a: Array<out Int>) {
    val r: Array<out Int?> = <!TYPE_MISMATCH!><!UNSUPPORTED!>bar<!>(a)<!>
    val t = <!UNSUPPORTED!>bar<!>(a)
    <!UNSUPPORTED!>t<!> checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><Array<out Int?>>() }
}

fun <T: Any> foo(l: Array<T>): Array<Array<T?>> = null!!

fun test2(a: Array<out Int>) {
    val r: Array<out Array<out Int?>> = <!TYPE_MISMATCH!><!UNSUPPORTED!>foo<!>(a)<!>
    val t = <!UNSUPPORTED!>foo<!>(a)
    <!UNSUPPORTED!>t<!> checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><Array<out Array<out Int?>>>() }
}
