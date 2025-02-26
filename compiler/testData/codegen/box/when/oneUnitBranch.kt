// ISSUE: KT-68449
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_2_0
// ^^^ Test failed with: kotlin.Unit

fun foo(x: Any): String {
    val result = when (x) {
        is String -> x.toString()
        is Long -> x + 10
        else -> {}
    }
    return result.toString()
}

fun box(): String {
    return foo("OK")
}
