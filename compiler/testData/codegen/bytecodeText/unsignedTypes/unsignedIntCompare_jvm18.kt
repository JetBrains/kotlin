// JVM_TARGET: 1.8
// WITH_RUNTIME
// IGNORE_BACKEND: JVM_IR
// TODO KT-36838 Use potentially intrinsified methods for unsigned available in JDK 1.8+ in JVM_IR

val ua = 1234U
val ub = 5678U

fun box(): String {
    if (ua.compareTo(ub) > 0) {
        throw AssertionError()
    }

    return "OK"
}

// 0 kotlin/UnsignedKt.uintCompare
// 1 INVOKESTATIC java/lang/Integer.compareUnsigned \(II\)I
