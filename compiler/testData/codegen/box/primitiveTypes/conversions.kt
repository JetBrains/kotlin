// WITH_STDLIB
import kotlin.test.assertEquals

fun box(): String {

    assertEquals(-2147483648, Float.NEGATIVE_INFINITY.toInt())
    assertEquals(-2147483648, (-Float.MAX_VALUE).toInt())
    assertEquals(-1, (-1.9f).toInt())
    assertEquals(0, (-0.0f).toInt())
    assertEquals(0, (-Float.MIN_VALUE).toInt())
    assertEquals(0, 0.0f.toInt())
    assertEquals(0, Float.NaN.toInt())
    assertEquals(0, Float.MIN_VALUE.toInt())
    assertEquals(0, 0.9f.toInt())
    assertEquals(2147483647, Float.MAX_VALUE.toInt())
    assertEquals(2147483647, Float.POSITIVE_INFINITY.toInt())

    assertEquals(-2147483648, Double.NEGATIVE_INFINITY.toInt())
    assertEquals(-2147483648, (-Double.MAX_VALUE).toInt())
    assertEquals(-2147483648, (-2147483649.0).toInt())
    assertEquals(-2147483648, (-2147483648.0).toInt())
    assertEquals(-2147483647, (-2147483647.9).toInt())
    assertEquals(-1, (-1.9).toInt())
    assertEquals(0, (-0.0).toInt())
    assertEquals(0, (-Double.MIN_VALUE).toInt())
    assertEquals(0, 0.0.toInt())
    assertEquals(0, Double.NaN.toInt())
    assertEquals(0, Double.MIN_VALUE.toInt())
    assertEquals(0, 0.9.toInt())
    assertEquals(2147483646, 2147483646.9.toInt())
    assertEquals(2147483647, 2147483647.0.toInt())
    assertEquals(2147483647, 2147483648.0.toInt())
    assertEquals(2147483647, Double.MAX_VALUE.toInt())
    assertEquals(2147483647, Double.POSITIVE_INFINITY.toInt())

    for (d in doubleArrayOf(Double.NEGATIVE_INFINITY, -Double.MAX_VALUE,
                            -2147483649.0, -2147483648.0, -2147483647.0,
                            -65536.0, -65535.0, -65534.0,
                            -1.5, -Double.MIN_VALUE,
                            -0.0, 0.0,
                            Double.MIN_VALUE, 1.5,
                            65534.0, 65535.0, 65536.0,
                            2147483647.0, 2147483648.0, 2147483649.0,
                            Double.MAX_VALUE, Double.POSITIVE_INFINITY)) {
        assertEquals(d.toInt().toChar(), d.toChar())

        val f = d.toFloat()
        assertEquals(f.toInt().toChar(), f.toChar())
    }

    return "OK"
}
