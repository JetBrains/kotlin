// JVM_TARGET: 1.8

fun box(): String {
    true.hashCode()
    1.toByte().hashCode()
    1.toChar().hashCode()
    1.toShort().hashCode()
    1.hashCode()
    1L.hashCode()
    1.0F.hashCode()
    1.0.hashCode()
    "".hashCode()

    return "OK"
}

// 9 \.hashCode
// 1 INVOKESTATIC java/lang/Boolean\.hashCode \(Z\)I
// 1 INVOKESTATIC java/lang/Byte\.hashCode \(B\)I
// 1 INVOKESTATIC java/lang/Character\.hashCode \(C\)I
// 1 INVOKESTATIC java/lang/Short\.hashCode \(S\)I
// 1 INVOKESTATIC java/lang/Integer\.hashCode \(I\)I
// 1 INVOKESTATIC java/lang/Long\.hashCode \(J\)I
// 1 INVOKESTATIC java/lang/Float\.hashCode \(F\)I
// 1 INVOKESTATIC java/lang/Double\.hashCode \(D\)I
// 1 INVOKEVIRTUAL java/lang/String\.hashCode \(\)I
