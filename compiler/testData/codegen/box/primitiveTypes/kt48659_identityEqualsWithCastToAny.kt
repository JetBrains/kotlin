// TARGET_BACKEND: JVM

fun int(a: Int, b: Int): Boolean = (a as Any) === (b as Any)
fun short(a: Short, b: Short): Boolean = (a as Any) === (b as Any)
fun char(a: Char, b: Char): Boolean = (a as Any) === (b as Any)
fun long(a: Long, b: Long): Boolean = (a as Any) === (b as Any)
fun float(a: Float, b: Float): Boolean = (a as Any) === (b as Any)
fun double(a: Double, b: Double): Boolean = (a as Any) === (b as Any)

fun byte(a: Byte, b: Byte): Boolean = (a as Any) === (b as Any)
fun boolean(a: Boolean, b: Boolean): Boolean = (a as Any) === (b as Any)

fun box(): String {
    if (int(2021, 2021)) return "Fail int"
    if (short(32042, 32042)) return "Fail short"
    if (char(4242.toChar(), 4242.toChar())) return "Fail char"
    if (long(4242424242L, 4242424242L)) return "Fail long"
    if (float(3.14f, 3.14f)) return "Fail float"
    if (double(2.72, 2.72)) return "Fail double"

    // All byte and boolean values are cached in java.lang.Byte/Boolean.valueOf
    if (!byte(127, 127)) return "Fail byte"
    if (!boolean(true, true)) return "Fail boolean"

    return "OK"
}
