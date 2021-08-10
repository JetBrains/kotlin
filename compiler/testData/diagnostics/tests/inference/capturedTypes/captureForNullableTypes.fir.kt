// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// !CHECK_TYPE

fun <T: Any> bar(a: Array<T>): Array<T?> =  null!!

fun test1(a: Array<out Int>) {
    val r: Array<out Int?> = <!INITIALIZER_TYPE_MISMATCH!>bar(a)<!>
    val t = bar(a)
    t checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><Array<out Int?>>() }
}

fun <T: Any> foo(l: Array<T>): Array<Array<T?>> = null!!

fun test2(a: Array<out Int>) {
    val r: Array<out Array<out Int?>> = <!INITIALIZER_TYPE_MISMATCH!>foo(a)<!>
    val t = foo(a)
    t checkType { <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>_<!><Array<out Array<out Int?>>>() }
}
