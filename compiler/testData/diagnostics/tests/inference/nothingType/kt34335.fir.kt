// !DIAGNOSTICS: -UNUSED_PARAMETER

fun call(vararg x: Any?) {}
fun <R> Any.call(vararg args: Any?): R = TODO()
fun println(message: Any?) {}

fun foo(action: (Int) -> Unit) {
    action(10)
}

fun test1() {
    call({ x -> println(x::class) }) // x inside the lambda is inferred to `Nothing`, the lambda is `(Nothing) -> Unit`.
}

fun test2() {
    ::foo.<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>call<!>({ x -> println(x::class) })
}
