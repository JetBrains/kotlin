// WITH_RUNTIME
// JVM_TARGET: 1.6

val ua = 1234U
val ub = 5678U

fun box(): String {
    if (ua.compareTo(ub) > 0) {
        throw AssertionError()
    }

    return "OK"
}

// 1 kotlin/UnsignedKt.uintCompare
// 0 INVOKESTATIC java/lang/Integer.compareUnsigned \(II\)I
