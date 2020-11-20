// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: SPREAD_OPERATOR
var result = "fail"

fun foo(vararg xs: Int, s1: String = "", s2: String = "OK") {
    if (xs[0] == 42 && s1 == "good") {
        result = s2
    }
}

fun bar(vararg xs: Int, s: String = "") {}

fun use(fn: (IntArray, String) -> Unit) {
    fn(intArrayOf(42), "good")
}

fun test() {
    use(::foo)
    use(::bar)
}

fun box(): String {
    test()
    return "OK"
}