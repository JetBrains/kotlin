// TARGET_BACKEND: WASM
// ENABLE_TAIL_CALLS

// The accumulator helper's self-call must be emitted as return_call.
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=countDown$accum
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=sumTo$accum
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=maskChain$accum
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=sumEvens$accum
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=repeatStr$accum
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=productChain$accum
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=andChain$accum
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=xorChain$accum
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=longProduct$accum
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=longAndChain$accum
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=longXorChain$accum
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=boolAndChain$accum
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=boolOrChain$accum
// WASM_CHECK_INSTRUCTION_IN_FUNCTION: instruction=return_call inFunction=boolXorChain$accum

// --- Int ---

// `1 + f(n - 1)`: counting, the canonical accumulator shape.
fun countDown(n: Int): Int {
    if (n == 0) return 0
    return 1 + countDown(n - 1)
}

// Multiple return sites sharing the same operator.
fun sumEvens(n: Int): Int {
    if (n == 0) return 0
    if (n % 2 == 0) return n + sumEvens(n - 1)
    return 0 + sumEvens(n - 1)
}

// Int.times
fun productChain(n: Int): Int {
    if (n == 0) return 1
    return 2 * productChain(n - 1)
}

// Int.and
fun andChain(n: Int, mask: Int): Int {
    if (n == 0) return mask
    return mask and andChain(n - 1, mask)
}

// Recursion on the left with a pure operand (Int.or).
fun maskChain(n: Int, bit: Int): Int {
    if (n == 0) return 0
    return maskChain(n - 1, bit) or bit
}

// Int.xor
fun xorChain(n: Int): Int {
    if (n == 0) return 0
    return 1 xor xorChain(n - 1)
}

// --- Long ---

// `n + f(n - 1)` with a Long accumulator.
fun sumTo(n: Long): Long {
    if (n == 0L) return 0L
    return n + sumTo(n - 1L)
}

// Long.times
fun longProduct(n: Int): Long {
    if (n == 0) return 1L
    return 3L * longProduct(n - 1)
}

// Long.and
fun longAndChain(n: Int, mask: Long): Long {
    if (n == 0) return mask
    return mask and longAndChain(n - 1, mask)
}

// Long.or: covered via symmetry with Int.or (maskChain).

// Long.xor
fun longXorChain(n: Int): Long {
    if (n == 0) return 0L
    return 1L xor longXorChain(n - 1)
}

// --- Boolean ---

// Boolean.and
fun boolAndChain(n: Int): Boolean {
    if (n == 0) return true
    return true and boolAndChain(n - 1)
}

// Boolean.or
fun boolOrChain(n: Int): Boolean {
    if (n == 0) return false
    return false or boolOrChain(n - 1)
}

// Boolean.xor
fun boolXorChain(n: Int): Boolean {
    if (n == 0) return false
    return true xor boolXorChain(n - 1)
}

// --- String ---

// String concatenation (associative, not commutative).
fun repeatStr(s: String, n: Int): String {
    if (n == 0) return ""
    return s + repeatStr(s, n - 1)
}

fun box(): String {
    if (countDown(1_000_000) != 1_000_000) return "fail countDown"

    // sumEvens adds 2 + 4 + ... + 1_000_000 with Int wrap-around; the expected
    // value is the same sum computed in Long and truncated, because Int `+` is
    // associative under wrap-around and the fold order does not change the result.
    val expectedEvens = ((2L + 1_000_000L) * 500_000L / 2L).toInt()
    if (sumEvens(1_000_000) != expectedEvens) return "fail sumEvens"

    // 2^20 = 1_048_576 (fits in Int).
    if (productChain(20) != 1_048_576) return "fail productChain"

    if (andChain(1_000_000, 0b1010) != 0b1010) return "fail andChain"

    if (maskChain(1_000_000, 0b101) != 0b101) return "fail maskChain"

    // xor with 1 an even number of times cancels out.
    if (xorChain(1_000_000) != 0) return "fail xorChain even"
    if (xorChain(999_999) != 1) return "fail xorChain odd"

    // 1M-term sum; Long avoids overflow so the closed form checks the fold order end to end.
    if (sumTo(1_000_000L) != 500_000_500_000L) return "fail sumTo"

    // 3^13 = 1_594_323 (fits in Long without overflow concerns).
    if (longProduct(13) != 1_594_323L) return "fail longProduct"

    if (longAndChain(1_000_000, 0xFF00L) != 0xFF00L) return "fail longAndChain"

    if (longXorChain(1_000_000) != 0L) return "fail longXorChain even"
    if (longXorChain(999_999) != 1L) return "fail longXorChain odd"

    // `true and true and ... and true` stays true.
    if (boolAndChain(1_000_000) != true) return "fail boolAndChain"

    // `false or false or ... or false` stays false.
    if (boolOrChain(1_000_000) != false) return "fail boolOrChain"

    // `true xor` applied an even number of times cancels out.
    if (boolXorChain(1_000_000) != false) return "fail boolXorChain even"
    if (boolXorChain(999_999) != true) return "fail boolXorChain odd"

    val expected = "ab".repeat(10_000)
    if (repeatStr("ab", 10_000) != expected) return "fail repeatStr"

    return "OK"
}
