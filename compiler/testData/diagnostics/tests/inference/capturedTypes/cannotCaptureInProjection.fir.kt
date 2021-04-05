// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// !CHECK_TYPE

fun <T: Any> bar(a: Array<T>): Array<T?> =  null!!

fun test1(a: Array<in Int>) {
    val r: Array<in Int?> = bar(<!ARGUMENT_TYPE_MISMATCH!>a<!>)
    bar(<!ARGUMENT_TYPE_MISMATCH!>a<!>)
}

fun <T: Any> foo(l: Array<T>): Array<Array<T?>> = null!!

fun test2(a: Array<in Int>) {
    val r: Array<out Array<in Int?>> = foo(<!ARGUMENT_TYPE_MISMATCH!>a<!>)
    foo(<!ARGUMENT_TYPE_MISMATCH!>a<!>)
}
