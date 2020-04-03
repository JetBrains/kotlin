// JVM_TARGET: 1.8
// WITH_RUNTIME
// IGNORE_BACKEND: JVM_IR
// TODO KT-36838 Use potentially intrinsified methods for unsigned available in JDK 1.8+ in JVM_IR

val ua = 1234UL
val ub = 5678UL
val uc = 3456UL
val u = ua * ub + uc

fun box(): String {
    val rem = u % ub
    if (rem != uc) throw AssertionError("$rem")

    return "OK"
}

// 0 kotlin/UnsignedKt.ulongRemainder
// 1 INVOKESTATIC java/lang/Long.remainderUnsigned \(JJ\)J