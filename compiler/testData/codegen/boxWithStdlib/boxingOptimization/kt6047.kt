import kotlin.test.assertEquals

fun box() : String {
    val x = java.lang.Long.valueOf("AB5E", 16)

    if (x != 0xAB5E.toLong()) return "fail 1: " + x.toString()

    val y = java.lang.Double.valueOf("1.0")

    if (y != 1.0) return "fail 2: " + y.toString()

    val c = java.lang.Byte.valueOf("A", 16);

    if (c != 10.toByte()) return "fail 3: " + c.toString()

    return "OK"
}
