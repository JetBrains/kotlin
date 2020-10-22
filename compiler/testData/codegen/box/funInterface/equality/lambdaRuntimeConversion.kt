// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: SAM_CONVERSIONS
// IGNORE_BACKEND: JS, JS_IR, JS_IR_ES6
// FILE: test.kt

fun checkEqual(x: Any, y: Any) {
    if (x != y || y != x) throw AssertionError("$x and $y should be equal")
    if (x.hashCode() != y.hashCode()) throw AssertionError("$x and $y should have the same hash code")
}

fun checkNotEqual(x: Any, y: Any) {
    if (x == y || y == x) throw AssertionError("$x and $y should NOT be equal")
}

fun interface FunInterface {
    fun invoke()
}

private fun id(f: FunInterface): Any = f

val lambda = {}

fun box(): String {
    checkEqual(id(lambda), id(lambda))
    checkEqual(id(lambda), lambdaFromOtherFile())
    return "OK"
}

// FILE: fromOtherFile.kt

private fun id(f: FunInterface): Any = f

fun lambdaFromOtherFile(): Any = id(lambda)
