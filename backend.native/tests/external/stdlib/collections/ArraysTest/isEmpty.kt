import kotlin.test.*

fun box() {
    assertTrue(emptyArray<String>().isEmpty())
    assertFalse(arrayOf("").isEmpty())
    assertTrue(intArrayOf().isEmpty())
    assertFalse(intArrayOf(1).isEmpty())
    assertTrue(byteArrayOf().isEmpty())
    assertFalse(byteArrayOf(1).isEmpty())
    assertTrue(shortArrayOf().isEmpty())
    assertFalse(shortArrayOf(1).isEmpty())
    assertTrue(longArrayOf().isEmpty())
    assertFalse(longArrayOf(1).isEmpty())
    assertTrue(charArrayOf().isEmpty())
    assertFalse(charArrayOf('a').isEmpty())
    assertTrue(floatArrayOf().isEmpty())
    assertFalse(floatArrayOf(0.1F).isEmpty())
    assertTrue(doubleArrayOf().isEmpty())
    assertFalse(doubleArrayOf(0.1).isEmpty())
    assertTrue(booleanArrayOf().isEmpty())
    assertFalse(booleanArrayOf(false).isEmpty())
}
