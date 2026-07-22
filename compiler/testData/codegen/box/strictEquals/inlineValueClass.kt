// LANGUAGE: +StrictEquals
// API_VERSION: 2.5
// TARGET_BACKEND: JVM_IR
// WITH_STDLIB

@JvmInline
value class W(val n: Int)

fun box(): String {
    val w1 = W(42)
    val w2 = W(42)
    val w3 = W(1)
    val any: Any? = W(42)
    if (w1 != w2) return "FAIL 1"
    if (w1 == w3) return "FAIL 2"
    if (w1 == any) {
        if (any.n != 42) return "FAIL 3"
    } else {
        return "FAIL 4"
    }
    return "OK"
}
