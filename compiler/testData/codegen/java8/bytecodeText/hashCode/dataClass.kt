// JVM_TARGET: 1.8

data class Hash(
        val a: Boolean,
        val b: Byte,
        val c: Char,
        val d: Short,
        val e: Int,
        val f: Long,
        val g: Float,
        val j: Double,
        val k: Any
)


// 8 \.hashCode
// 0 INVOKESTATIC java/lang/Boolean\.hashCode \(Z\)I
// 1 INVOKESTATIC java/lang/Byte\.hashCode \(B\)I
// 1 INVOKESTATIC java/lang/Character\.hashCode \(C\)I
// 1 INVOKESTATIC java/lang/Short\.hashCode \(S\)I
// 1 INVOKESTATIC java/lang/Integer\.hashCode \(I\)I
// 1 INVOKESTATIC java/lang/Long\.hashCode \(J\)I
// 1 INVOKESTATIC java/lang/Float\.hashCode \(F\)I
// 1 INVOKESTATIC java/lang/Double\.hashCode \(D\)I
// 1 INVOKEVIRTUAL java/lang/Object\.hashCode \(\)I
