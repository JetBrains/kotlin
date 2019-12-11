// !WITH_NEW_INFERENCE
fun <T> Array<T>.foo() {}

fun test(array: Array<out Int>) {
    array.foo()
    array.<!INAPPLICABLE_CANDIDATE!>foo<!><out Int>()
}