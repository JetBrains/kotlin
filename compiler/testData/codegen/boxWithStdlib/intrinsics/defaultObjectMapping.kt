import kotlin.test.assertEquals

fun box(): String {
    assertEquals(java.lang.Integer.MIN_VALUE, Int.MIN_VALUE)
    assertEquals(java.lang.Byte.MAX_VALUE, Byte.MAX_VALUE)

    assertEquals("MIN_VALUE", (Int.Default::MIN_VALUE).name)
    assertEquals("MAX_VALUE", (Double.Default::MAX_VALUE).name)
    assertEquals("MIN_VALUE", (Float.Default::MIN_VALUE).name)
    assertEquals("MAX_VALUE", (Long.Default::MAX_VALUE).name)
    assertEquals("MIN_VALUE", (Short.Default::MIN_VALUE).name)
    assertEquals("MAX_VALUE", (Byte.Default::MAX_VALUE).name)

    return "OK"
}
