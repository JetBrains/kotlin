// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: BIG_ARITY
// DONT_TARGET_EXACT_BACKEND: WASM
// WASM_MUTE_REASON: BIG_ARITY
// IGNORE_BACKEND: NATIVE
var result = "Fail"

fun foo(
    a01: Int, a02: Int, a03: Int, a04: Int, a05: Int, a06: Int, a07: Int, a08: Int, a09: Int, a10: Int,
    a11: Int, a12: Int, a13: Int, a14: Int, a15: Int, a16: Int, a17: Int, a18: Int, a19: Int, a20: Int,
    a21: Int, a22: Int, a23: Int, a24: Int, a25: Int, a26: Int, a27: Int, a28: Int, a29: Int, a30: Int,
    a31: Int, a32: Int, a33: Int, a34: Int, a35: Int, a36: Int, a37: Int, a38: Int, a39: Int, a40: Int
) {
    result = "OK"
}

fun box(): String {
    val x = ::foo
    x(
        1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
        11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
        21, 22, 23, 24, 25, 26, 27, 28, 29, 30,
        31, 32, 33, 34, 35, 36, 37, 38, 39, 40
    )
    return result
}
