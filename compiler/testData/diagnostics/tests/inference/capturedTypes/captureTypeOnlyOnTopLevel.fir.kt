// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_VARIABLE

fun <T> foo(array: Array<Array<T>>): Array<Array<T>> = array

fun test(array: Array<Array<out Int>>) {
    <!INAPPLICABLE_CANDIDATE!>foo<!>(array)

    val f: Array<out Array<out Int>> = <!INAPPLICABLE_CANDIDATE!>foo<!>(array)
}
