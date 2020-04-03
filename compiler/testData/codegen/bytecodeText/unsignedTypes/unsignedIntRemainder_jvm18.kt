// JVM_TARGET: 1.8
// WITH_RUNTIME
// IGNORE_BACKEND: JVM_IR
// TODO KT-36838 Use potentially intrinsified methods for unsigned available in JDK 1.8+ in JVM_IR

val ua = 1234U
val ub = 5678U
val uc = 3456U
val u = ua * ub + uc

fun box(): String {
    val rem = u % ub
    if (rem != uc) throw AssertionError("$rem")

    return "OK"
}

// 0 kotlin/UnsignedKt.uintRemainder
// 1 INVOKESTATIC java/lang/Integer.remainderUnsigned \(II\)I
