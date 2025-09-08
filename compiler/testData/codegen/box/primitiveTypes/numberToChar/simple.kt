// ISSUE: KT-46465
// WITH_STDLIB

class MyNumber(val value: Int) : Number() {
    override fun toInt(): Int = value

    override fun toByte(): Byte = toInt().toByte()
    override fun toDouble(): Double = toInt().toDouble()
    override fun toFloat(): Float = toInt().toFloat()
    override fun toLong(): Long = toInt().toLong()
    override fun toShort(): Short = toInt().toShort()
}

fun box(): String {
    val x = MyNumber('*'.code).toInt().toChar()
    return if (x == '*') "OK" else "Fail: $x"
}
