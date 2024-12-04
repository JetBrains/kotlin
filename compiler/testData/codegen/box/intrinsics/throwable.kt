// TARGET_BACKEND: JVM
// TARGET_BACKEND: JVM_IR

fun box(): String {
    val s: String? = "OK"
    val t: Throwable? = Throwable("test", null)

    val z = Throwable(s, t)
    if (z.message !== s) return "fail 1: ${z.message}"
    if (z.cause !== t) return "fail 2: ${z.cause}"

    val z2 = Throwable(s)
    if (z2.message !== s) return "fail 3: ${z2.message}"
    if (z2.cause !== null) return "fail 4: ${z2.cause}"

    val z3 = Throwable(t)
    if (z3.message != "java.lang.Throwable: test") return "fail 5: ${z3.message}"
    if (z3.cause !== t) return "fail 6: ${z2.cause}"

    val z4 = Throwable()
    if (z4.message !== null) return "fail 7: ${z4.message}"
    if (z4.cause !== null) return "fail 8: ${z4.cause}"

    return z.message!!
}
