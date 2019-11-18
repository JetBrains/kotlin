// IGNORE_BACKEND_FIR: JVM_IR
fun testBoolean(v: Boolean): Boolean {
    var value = false
    fun setValue(v: Boolean) {
        value = v
    }
    setValue(v)
    return value
}

fun testChar(v: Char): Char {
    var value = 0.toChar()
    fun setValue(v: Char) {
        value = v
    }
    setValue(v)
    return value
}

fun testByte(v: Byte): Byte {
    var value = 0.toByte()
    fun setValue(v: Byte) {
        value = v
    }
    setValue(v)
    return value
}

fun testShort(v: Short): Short {
    var value = 0.toShort()
    fun setValue(v: Short) {
        value = v
    }
    setValue(v)
    return value
}

fun testInt(v: Int): Int {
    var value = 0.toInt()
    fun setValue(v: Int) {
        value = v
    }
    setValue(v)
    return value
}

fun testLong(v: Long): Long {
    var value = 0.toLong()
    fun setValue(v: Long) {
        value = v
    }
    setValue(v)
    return value
}

fun testFloat(v: Float): Float {
    var value = 0.toFloat()
    fun setValue(v: Float) {
        value = v
    }
    setValue(v)
    return value
}

fun testDouble(v: Double): Double {
    var value = 0.toDouble()
    fun setValue(v: Double) {
        value = v
    }
    setValue(v)
    return value
}

fun box(): String {
    return when {
        testBoolean(true) != true -> "testBoolean"
        testChar('a') != 'a' -> "testChar"
        testByte(1) != 1.toByte() -> "testByte"
        testShort(1) != 1.toShort() -> "testShort"
        testInt(1) != 1 -> "testInt"
        testLong(1) != 1L -> "testLong"
        testFloat(1.0F) != 1.0F -> "testFloat"
        testDouble(1.0) != 1.0 -> "testDouble"
        else -> "OK"
    }
}