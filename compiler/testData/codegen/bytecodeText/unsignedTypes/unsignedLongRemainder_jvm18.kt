// JVM_TARGET: 1.8
// WITH_STDLIB

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