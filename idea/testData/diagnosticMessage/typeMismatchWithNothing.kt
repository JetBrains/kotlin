// !DIAGNOSTICS_NUMBER: 1
// !DIAGNOSTICS: TYPE_MISMATCH
// !LANGUAGE: -NewInference

package b

interface A<T>

fun <T> infer(a: A<T>) : T {}

fun foo(nothing: Nothing?) {
    val i = infer(nothing)
}
