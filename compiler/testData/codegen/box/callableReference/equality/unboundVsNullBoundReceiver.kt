// IGNORE_BACKEND: WASM_JS, WASM_WASI
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: Native:2.3,2.4
// ^ Fixed in 2.5 by KT-87445
// ISSUE: KT-87625

fun checkNotEqual(x: Any, y: Any) {
    if (x == y || y == x) throw AssertionError("$x and $y should NOT be equal")
}

fun Int?.foo(x: String = "") {}

fun box(): String {
    val unbound: (Int) -> Unit = Int::foo
    val nullBound: (String) -> Unit = null::foo

    checkNotEqual(unbound, nullBound)

    return "OK"
}
