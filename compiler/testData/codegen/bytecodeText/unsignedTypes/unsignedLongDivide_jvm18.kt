// JVM_TARGET: 1.8
// WITH_RUNTIME
// IGNORE_BACKEND: JVM_IR
// TODO KT-36838 Use potentially intrinsified methods for unsigned available in JDK 1.8+ in JVM_IR

val ua = 1234UL
val ub = 5678UL
val u = ua * ub

fun box(): String {
    val div = u / ua
    if (div != ub) throw AssertionError("$div")

    return "OK"
}

// 0 kotlin/UnsignedKt.ulongDivide
// 1 INVOKESTATIC java/lang/Long.divideUnsigned \(JJ\)J