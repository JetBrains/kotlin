// DONT_TARGET_EXACT_BACKEND: JVM
// DONT_TARGET_EXACT_BACKEND: JS
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// ISSUE: KT-46465
// WITH_STDLIB

class MyNumber(val value: Int) : Number() {
    override fun toChar(): Char = super.toChar()
    override fun toInt(): Int = value

    override fun toByte(): Byte = toInt().toByte()
    override fun toDouble(): Double = toInt().toDouble()
    override fun toFloat(): Float = toInt().toFloat()
    override fun toLong(): Long = toInt().toLong()
    override fun toShort(): Short = toInt().toShort()
}

fun box(): String {
    val x = MyNumber('*'.code).toChar()
    return if (x == '*') "OK" else "Fail: $x"
}
