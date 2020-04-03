// JVM_TARGET: 1.8
// WITH_RUNTIME
// IGNORE_BACKEND: JVM_IR
// TODO KT-36838 Use potentially intrinsified methods for unsigned available in JDK 1.8+ in JVM_IR

val ua = 1234UL
val ub = 5678UL

fun box(): String {
    if (ua.compareTo(ub) > 0) {
        throw AssertionError()
    }

    return "OK"
}

// 0 kotlin/UnsignedKt.ulongCompare
// 1 INVOKESTATIC java/lang/Long.compareUnsigned \(JJ\)I
