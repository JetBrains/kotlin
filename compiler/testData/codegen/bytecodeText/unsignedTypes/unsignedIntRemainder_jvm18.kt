// JVM_TARGET: 1.8
// WITH_STDLIB

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
