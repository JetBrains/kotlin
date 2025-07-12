// ISSUE: KT-72446
// NO_CHECK_LAMBDA_INLINING
// FILE: lib.kt
inline fun test(
    block: () -> String = {
        var result = "O"
        val temp = { result += "K" }
        temp()
        result
    }
) = block()

// FILE: main.kt
fun box() = test()
