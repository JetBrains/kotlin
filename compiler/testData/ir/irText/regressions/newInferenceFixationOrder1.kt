// FIR_IDENTICAL
// IGNORE_BACKEND_K2: JS_IR
// IGNORE_BACKEND_K2: JS_IR_ES6
fun <X, Y> foo(): (X) -> Y = TODO()

interface Inv2<A, B>

fun <T, R> check(x: T, y: R, f: (T) -> R): Inv2<T, R> = TODO()

fun test() = check("", 1, foo())

fun box(): String {
    val x: Inv2<String, Int> = test()

    return "OK"
}
