// ISSUE: KT-76024
// FILE: 1.kt
inline fun test(param: String) = param.let { it.let { it } }

// FILE: 2.kt
fun box() = test("OK")
