// VALHALLA_VALUE_CLASSES
// LANGUAGE: +FullValueClasses

value class ValueNumber(val x: Int) : Number() {
    override fun toByte(): Byte = x.toByte()
    override fun toDouble(): Double = x.toDouble()
    override fun toFloat(): Float = x.toFloat()
    override fun toInt(): Int = x
    override fun toLong(): Long = x.toLong()
    override fun toShort(): Short = x.toShort()
}

fun box(): String {
    val number: Number = ValueNumber(42)
    if (!number.javaClass.isValue) return "ValueNumber is not a value class"
    if (number.toInt() != 42 || number.toDouble() != 42.0) return "number: ${number.toInt()}, ${number.toDouble()}"
    if (number != ValueNumber(42)) return "equals"
    return "OK"
}
