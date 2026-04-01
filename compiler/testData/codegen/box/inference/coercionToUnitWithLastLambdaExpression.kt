// ISSUE: KT-82732
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: WASM-JS:2.3.0
// ^^^ K/Wasm backend v.2.3.0 has issue KT-82732, fixed only in 2.3.20-Beta2. So, a test `current frontend + 2.3.0 backend` expectedly fails
fun <T> myRun(action: () -> T): T = action()
fun foo(): String = "foo"

fun <K> materialize(): K {
    result += "K"
    return "str" as K
}

var result = "fail"

fun test1(n: Number, b: Boolean) {
    n.let {
        if (b) return@let

        myRun {
            result = "O"
            foo()
        }
    }
}

fun test2(n: Number, b: Boolean) {
    n.let {
        if (b) return@let
        materialize()
    }
}

fun box(): String {
    test1(42, false)
    test2(42, false)
    return result
}