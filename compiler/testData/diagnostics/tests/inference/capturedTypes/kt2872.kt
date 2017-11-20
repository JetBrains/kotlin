// !WITH_NEW_INFERENCE
fun <T> Array<T>.foo() {}

fun test(array: Array<out Int>) {
    array.foo()
    <!TYPE_MISMATCH!>array<!>.foo<<!PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT!>out<!> Int>()
}