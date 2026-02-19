// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// ^ KT-79282

object FortyTwo: Number() {
    override fun toDouble() = 42.0
    override fun toFloat() = 42.0f
    override fun toLong() = 42L
    override fun toInt() = 42
    override fun toShort() = 42.toShort()
    override fun toByte() = 42.toByte()
}

fun numberToDouble(n: Number) = n.toDouble()
fun numberToFloat(n: Number) = n.toFloat()
fun numberToLong(n: Number) = n.toLong()
fun numberToInt(n: Number) = n.toInt()
fun numberToShort(n: Number) = n.toShort()
fun numberToByte(n: Number) = n.toByte()

fun box(): String {

    var result = "\n"
    result += "toDouble: ${numberToDouble(FortyTwo)}\n"
    result += "toFloat: ${numberToFloat(FortyTwo)}\n"
    result += "toLong: ${numberToLong(FortyTwo)}\n"
    result += "toInt: ${numberToInt(FortyTwo)}\n"
    result += "toShort: ${numberToShort(FortyTwo)}\n"
    result += "toByte: ${numberToByte(FortyTwo)}\n"

    val expected = """
toDouble: 42.0
toFloat: 42.0
toLong: 42
toInt: 42
toShort: 42
toByte: 42
"""

    if (result != expected) {
        return result
    }

    return "OK"
}
