// JVM_TARGET: 1.8
// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME

class D(val x: UInt?)

class E(val x: Any)

fun f(d: D): String {
    return d.x?.let { d.x.toString() } ?: ""
}

fun g(e: E): String {
    if (e.x is UInt) return e.x.toString()
    return ""
}

fun box(): String {
    val test1 = f(D(42u))
    if (test1 != "42") throw Exception(test1)

    val test2 = g(E(42u))
    if (test2 != "42") throw Exception(test2)

    return "OK"
}