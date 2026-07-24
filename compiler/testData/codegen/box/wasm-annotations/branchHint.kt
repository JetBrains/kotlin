// TARGET_BACKEND: WASM
// RUN_THIRD_PARTY_OPTIMIZER
// DISABLE_IR_VISIBILITY_CHECKS: ANY
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: Wasm-JS:2.4
// ^^^ KT-84667 is fixed in 2.4.20-Beta1

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

import kotlin.random.Random
import kotlin.wasm.internal.*

fun box(): String {
    val r = Random.nextInt(100)
    var result = ""

    // Explicit function-based hint for the branch condition
    if (likely(r > 48)) {
        result += "a"
    } else {
        result += "b"
    }

    // Non-taken branch with unlikely hint
    if (unlikely(r > 51)) {
        result += "c"
    } else {
        result += "d"
    }

    var result2 = 0

    while (likely(Random.nextInt(10) != 0)) {
        result2 += 1
    }

    while (unlikely(Random.nextInt(10) == 0)) {
        result2 -= 1
    }

    do {
        result2 += 1
    } while (likely(Random.nextInt(10) != 0)
    )

    do {
        result2 -= 1
    } while (unlikely(Random.nextInt(10) == 0))

    if (result == "bc") return "Fail"
    println(result2)

    return "OK"
}
