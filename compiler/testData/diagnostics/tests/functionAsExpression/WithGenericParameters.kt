// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER

interface A
fun devNull(a: Any?){}

val generic_fun = fun<!TYPE_PARAMETERS_NOT_ALLOWED!><T><!>(t: T): T = null!!
val extension_generic_fun = fun<!TYPE_PARAMETERS_NOT_ALLOWED!><T><!>T.(t: T): T = null!!

fun fun_with_where() = fun <!TYPE_PARAMETERS_NOT_ALLOWED!><T><!> T.(t: T): T where T: A = null!!


fun outer() {
    devNull(fun <!TYPE_PARAMETERS_NOT_ALLOWED!><T><!>() {})
    devNull(fun <!TYPE_PARAMETERS_NOT_ALLOWED!><T><!> <!NI;UNRESOLVED_REFERENCE!>T<!>.() {})
    devNull(fun <!TYPE_PARAMETERS_NOT_ALLOWED!><T><!> (): <!NI;UNRESOLVED_REFERENCE!>T<!> = null!!)
    devNull(fun <!TYPE_PARAMETERS_NOT_ALLOWED!><T><!> (t: <!NI;UNRESOLVED_REFERENCE!>T<!>) {})
    devNull(fun <!TYPE_PARAMETERS_NOT_ALLOWED!><T><!> () where T:A {})
}