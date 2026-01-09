// FILE: lib.kt
@Suppress("RECURSION_IN_INLINE")
inline fun test(p: String = test("OK")): String {
    return p
}

// FILE: main.kt
fun box() : String {
    return test()
}
