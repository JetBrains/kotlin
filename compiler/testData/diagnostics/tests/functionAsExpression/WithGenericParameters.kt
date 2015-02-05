// !DIAGNOSTICS: -UNUSED_PARAMETER

trait A
fun devNull(a: Any?){}

val generic_fun = fun<!TYPE_PARAMETERS_NOT_ALLOWED!><T><!>(t: T): T = null!!
val extension_generic_fun = fun<!TYPE_PARAMETERS_NOT_ALLOWED!><T><!><!UNRESOLVED_REFERENCE!>T<!>.(t: T): T = null!!

fun fun_with_where() = fun <!TYPE_PARAMETERS_NOT_ALLOWED!><T><!> <!UNRESOLVED_REFERENCE!>T<!>.(t: T): T where T: A {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>


fun outer() {
    devNull(fun <!TYPE_PARAMETERS_NOT_ALLOWED!><T><!>() {})
    devNull(fun <!TYPE_PARAMETERS_NOT_ALLOWED!><T><!> T.name() {})
    devNull(fun <!TYPE_PARAMETERS_NOT_ALLOWED!><T><!> name(): T = null!!)
    devNull(fun <!TYPE_PARAMETERS_NOT_ALLOWED!><T><!> name(t: T) {})
    devNull(fun <!TYPE_PARAMETERS_NOT_ALLOWED!><T><!> name() where T:A {})
}