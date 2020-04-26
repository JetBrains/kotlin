// IGNORE_BACKEND: JS, JS_IR, NATIVE
// FILE: test.kt

fun checkEqual(x: Any, y: Any) {
    if (x != y || y != x) throw AssertionError("$x and $y should be equal")
    if (x.hashCode() != y.hashCode()) throw AssertionError("$x and $y should have the same hash code")
}

fun target(x: Int) {}

fun use(fn: (Int) -> Unit): Any = fn

fun box(): String {
    checkEqual(use(::target), ::target)
    checkEqual(useFromOtherFile(), ::target)
    return "OK"
}

// FILE: fromOtherFile.kt

fun useFromOtherFile(): Any = use(::target)

// DONT_TARGET_EXACT_BACKEND: WASM
//DONT_TARGET_WASM_REASON: FAILS_IN_JS_IR
