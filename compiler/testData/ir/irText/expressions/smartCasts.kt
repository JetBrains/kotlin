// FIR_IDENTICAL
// WITH_STDLIB
// IGNORE_BACKEND: JS_IR

// KT-61141: `println (message: kotlin.Any?)` instead of `println (message: kotlin.Int)`
// IGNORE_BACKEND: NATIVE
fun expectsString(s: String) {}
fun expectsInt(i: Int) {}

fun overloaded(s: String) = s
fun overloaded(x: Any) = x

fun test1(x: Any) {
    if (x !is String) return
    println(x.length)
    expectsString(x)
    expectsInt(x.length)
    expectsString(overloaded(x))
}

fun test2(x: Any): String {
    if (x !is String) return ""
    return overloaded(x)
}

fun test3(x: Any): String {
    if (x !is String) return ""
    return x
}
