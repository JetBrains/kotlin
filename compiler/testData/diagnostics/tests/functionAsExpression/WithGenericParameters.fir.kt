// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_ANONYMOUS_PARAMETER

interface A
fun devNull(a: Any?){}

val generic_fun = fun<T>(t: T): T = null!!
val extension_generic_fun = fun<T>T.(t: T): T = null!!

fun fun_with_where() = fun <T> T.(t: T): T where T: A = null!!


fun outer() {
    devNull(fun <T>() {})
    devNull(fun <T> T.() {})
    devNull(fun <T> (): T = null!!)
    devNull(fun <T> (t: T) {})
    devNull(fun <T> () where T:A {})
}