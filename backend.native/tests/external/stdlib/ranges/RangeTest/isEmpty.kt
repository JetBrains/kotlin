import kotlin.test.*


fun box() {
    assertTrue((2..1).isEmpty())
    assertTrue((2L..0L).isEmpty())
    assertTrue((1.toShort()..-1.toShort()).isEmpty())
    assertTrue((0.toByte()..-1.toByte()).isEmpty())
    assertTrue((0f..-3.14f).isEmpty())
    assertTrue((-2.72..-3.14).isEmpty())
    assertTrue(('z'..'x').isEmpty())

    assertTrue((1 downTo 2).isEmpty())
    assertTrue((0L downTo 2L).isEmpty())
    assertFalse((2 downTo 1).isEmpty())
    assertFalse((2L downTo 0L).isEmpty())
    assertTrue(('a' downTo 'z').isEmpty())
    assertTrue(('z'..'a' step 2).isEmpty())

    assertTrue(("range".."progression").isEmpty())
}
