// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// ISSUE: KT-46465
// WITH_STDLIB

class MyNumber(val value: Int) : Number() {
    override fun toInt(): Int = value

    override fun toDouble(): Double = toInt().toDouble()
    override fun toFloat(): Float = toInt().toFloat()
    override fun toLong(): Long = toInt().toLong()
}

fun box(): String {
    val x = MyNumber('*'.code).toChar()
    return if (x == '*') "OK" else "Fail: $x"
}
