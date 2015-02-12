// !DIAGNOSTICS_NUMBER: 1
// !DIAGNOSTICS: TYPE_INFERENCE_CANNOT_CAPTURE_TYPES

class My<R, T>

fun <R, T> foo(my: My<Array<R>, T>): My<Array<R>, T> = my

fun test11(my: My<Array<out Int>, out Int>) {
    foo(my)
}