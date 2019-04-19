// JVM_TARGET: 1.6
// WITH_RUNTIME
// IGNORE_BACKEND: JVM_IR

val ua = 1234U
val ub = 5678U

fun box(): String {
    if (ua.compareTo(ub) > 0) {
        throw AssertionError()
    }

    return "OK"
}

// 1 INVOKESTATIC kotlin/UnsignedKt.uintCompare
