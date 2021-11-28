// !DIAGNOSTICS: -UNUSED_PARAMETER

fun Int.invoke() {}

class SomeClass

fun test(identifier: SomeClass, fn: String.() -> Unit) {
    <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>identifier<!>()
    identifier(<!TOO_MANY_ARGUMENTS!>123<!>)
    identifier(<!TOO_MANY_ARGUMENTS!>1<!>, <!TOO_MANY_ARGUMENTS!>2<!>)
    <!ARGUMENT_TYPE_MISMATCH!>1<!>.fn()
}
