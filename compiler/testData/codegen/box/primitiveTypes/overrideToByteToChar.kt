// ISSUE: KT-16087
// ISSUE: KT-75767
// WITH_STDLIB

class MyNumber : Number() {
    override fun toShort(): Short = 1
    override fun toByte(): Byte = 1

    override fun toInt(): Int = null!!
    override fun toDouble(): Double = null!!
    override fun toFloat(): Float = null!!
    override fun toLong(): Long = null!!
}

fun box(): String {
    val x = MyNumber().toByte()
    val y = MyNumber().toShort()
    val z: Int? = 1
    return if (x == 1.toByte() && y == 1.toShort() && z?.toShort() == 1.toShort() && z.toByte() == 1.toByte()) "OK" else "Fail"
}