// JVM_TARGET: 1.8
// WITH_STDLIB

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