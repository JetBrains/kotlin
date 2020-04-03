// !WITH_NEW_INFERENCE
fun <T> Array<T>.foo() {}

fun test(array: Array<out Int>) {
    array.foo()
    <!OI;TYPE_MISMATCH!>array<!>.<!NI;UNRESOLVED_REFERENCE_WRONG_RECEIVER!>foo<!><<!PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT!>out<!> Int>()
}