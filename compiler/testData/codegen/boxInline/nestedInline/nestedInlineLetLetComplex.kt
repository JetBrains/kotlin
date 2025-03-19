// ISSUE: KT-76024
// FILE: 1.kt
inline fun test(param: String, lambda: (String) -> String) = param.let(lambda)

inline fun fullTest(param: String) = test (param) { it.let { it } }

// FILE: 2.kt
fun box() = fullTest("OK")
