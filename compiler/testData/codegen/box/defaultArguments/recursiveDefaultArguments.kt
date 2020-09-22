// IGNORE_BACKEND: JS, JS_IR
@Suppress("RECURSION_IN_INLINE")
inline fun test(p: String = test("OK")): String {
    return p
}

fun box() : String {
    return test()
}