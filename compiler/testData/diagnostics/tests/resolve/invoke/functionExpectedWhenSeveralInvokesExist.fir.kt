// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun Int.invoke(a: Int) {}
fun Int.invoke(a: Int, b: Int) {}

class SomeClass

fun test(identifier: SomeClass, fn: String.() -> Unit) {
    <!NONE_APPLICABLE!>identifier<!>()
    identifier(123<!NO_VALUE_FOR_PARAMETER!>)<!>
    identifier(1, <!TOO_MANY_ARGUMENTS!>2<!>)
    <!ARGUMENT_TYPE_MISMATCH!>1<!>.fn()
}
