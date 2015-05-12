// !DIAGNOSTICS_NUMBER: 1
// !DIAGNOSTICS: TYPE_MISMATCH

package b

interface A<T>

fun infer<T>(a: A<T>) : T {}

fun foo(nothing: Nothing?) {
    val i = infer(nothing)
}
