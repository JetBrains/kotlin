import kotlin.test.*


fun box() {
    assertTrue(IntRange.EMPTY == IntRange.EMPTY)
    assertEquals(IntRange.EMPTY, IntRange.EMPTY)
    assertEquals(0L..42L, 0L..42L)
    assertEquals(0L..4200000042000000L, 0L..4200000042000000L)
    assertEquals(3 downTo 0, 3 downTo 0)

    assertEquals(2..1, 1..0)
    assertEquals(2L..1L, 1L..0L)
    assertEquals(2.toShort()..1.toShort(), 1.toShort()..0.toShort())
    assertEquals(2.toByte()..1.toByte(), 1.toByte()..0.toByte())
    assertEquals(0f..-3.14f, 3.14f..0f)
    assertEquals(-2.0..-3.0, 3.0..2.0)
    assertEquals('b'..'a', 'c'..'b')

    assertTrue(1 downTo 2 == 2 downTo 3)
    assertTrue(-1L downTo 0L == -2L downTo -1L)
    assertEquals('j'..'a' step 4, 'u'..'q' step 2)

    assertFalse(0..1 == IntRange.EMPTY)

    assertEquals("range".."progression", "hashcode".."equals")
    assertFalse(("aa".."bb") == ("aaa".."bbb"))
}
