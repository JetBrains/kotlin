// IGNORE_BACKEND: JS
// JVM_ABI_K1_K2_DIFF: KT-62464

@Suppress("RECURSION_IN_INLINE")
inline fun test(p: String = test("OK")): String {
    return p
}

fun box() : String {
    return test()
}