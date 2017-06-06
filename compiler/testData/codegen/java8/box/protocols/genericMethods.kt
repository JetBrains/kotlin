// JVM_TARGET: 1.8
// WITH_RUNTIME
// FULL_JDK

protocol interface Super {
    fun <T> foo(t: T): T
}

class Z {
    fun <X> foo(b : X) = b
    fun foo(a: String) = a
}

fun box(): String {
    val x: Super = Z()

    if (x.foo("OK") != "OK") {
        return "FAIL"
    }

    if (x.foo(5) != 5) {
        return "FAIL"
    }

    return "OK"
}
