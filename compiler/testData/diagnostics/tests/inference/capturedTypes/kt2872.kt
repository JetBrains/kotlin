// !WITH_NEW_INFERENCE
fun <T> Array<T>.foo() {}

fun test(array: Array<out Int>) {
    array.foo()
    <!TYPE_MISMATCH{OI}!>array<!>.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER{NI}!>foo<!><<!PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT!>out<!> Int>()
}
