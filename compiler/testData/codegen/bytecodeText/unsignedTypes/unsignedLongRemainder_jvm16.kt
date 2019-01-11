// JVM_TARGET: 1.6
// WITH_RUNTIME
// IGNORE_BACKEND: JVM_IR

val ua = 1234UL
val ub = 5678UL
val uc = 3456UL
val u = ua * ub + uc

fun box(): String {
    val rem = u % ub
    if (rem != uc) throw AssertionError("$rem")

    return "OK"
}

// 1 kotlin/UnsignedKt.ulongRemainder