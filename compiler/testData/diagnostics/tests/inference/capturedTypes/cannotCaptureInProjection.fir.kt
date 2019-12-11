// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// !CHECK_TYPE

fun <T: Any> bar(a: Array<T>): Array<T?> =  null!!

fun test1(a: Array<in Int>) {
    val r: Array<in Int?> = <!INAPPLICABLE_CANDIDATE!>bar<!>(a)
    <!INAPPLICABLE_CANDIDATE!>bar<!>(a)
}

fun <T: Any> foo(l: Array<T>): Array<Array<T?>> = null!!

fun test2(a: Array<in Int>) {
    val r: Array<out Array<in Int?>> = <!INAPPLICABLE_CANDIDATE!>foo<!>(a)
    <!INAPPLICABLE_CANDIDATE!>foo<!>(a)
}