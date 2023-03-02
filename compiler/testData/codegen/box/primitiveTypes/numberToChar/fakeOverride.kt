// DONT_TARGET_EXACT_BACKEND: JVM
// DONT_TARGET_EXACT_BACKEND: JS
// ISSUE: KT-46465
// WITH_STDLIB

open class MyNumber(val value: Int) : Number() {
    override fun toChar(): Char = '+'
    override fun toInt(): Int = value

    override fun toByte(): Byte = toInt().toByte()
    override fun toDouble(): Double = toInt().toDouble()
    override fun toFloat(): Float = toInt().toFloat()
    override fun toLong(): Long = toInt().toLong()
    override fun toShort(): Short = toInt().toShort()
}

class MyNumberImpl(value: Int) : MyNumber(value)

fun box(): String {
    val x = MyNumberImpl('*'.code).toChar()
    return if (x == '+') "OK" else "Fail: $x"
}
