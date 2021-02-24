// !LANGUAGE: +NewInference
// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun call(vararg x: Any?) {}
fun <R> Any.call(vararg args: Any?): R = TODO()
fun println(message: Any?) {}

fun foo(action: (Int) -> Unit) {
    action(10)
}

fun test1() {
    call({ <!CANNOT_INFER_PARAMETER_TYPE{NI}!>x<!> -> println(<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE{NI}!>x<!>::class) }) // x inside the lambda is inferred to `Nothing`, the lambda is `(Nothing) -> Unit`.
}

fun test2() {
    ::foo.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER{NI}!>call<!>({ <!CANNOT_INFER_PARAMETER_TYPE{NI}!>x<!> -> println(<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE{NI}!>x<!>::class) })
}
