// DONT_TARGET_EXACT_BACKEND: WASM
// WITH_RUNTIME

fun p() {}

fun box(): String {
    var sum = 1
    for (i: Int? in sum downTo sum.toULong().countTrailingZeroBits())
        p()
    return "OK"
}
