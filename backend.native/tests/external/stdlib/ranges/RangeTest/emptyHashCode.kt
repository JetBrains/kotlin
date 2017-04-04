import kotlin.test.*


fun box() {
    assertEquals((0..42).hashCode(), (0..42).hashCode())
    assertEquals((1.23..4.56).hashCode(), (1.23..4.56).hashCode())

    assertEquals((0..-1).hashCode(), IntRange.EMPTY.hashCode())
    assertEquals((2L..1L).hashCode(), (1L..0L).hashCode())
    assertEquals((0.toShort()..-1.toShort()).hashCode(), (42.toShort()..0.toShort()).hashCode())
    assertEquals((0.toByte()..-1.toByte()).hashCode(), (42.toByte()..0.toByte()).hashCode())
    assertEquals((0f..-3.14f).hashCode(), (2.39f..1.41f).hashCode())
    assertEquals((0.0..-10.0).hashCode(), (10.0..0.0).hashCode())
    assertEquals(('z'..'x').hashCode(), ('l'..'k').hashCode())

    assertEquals((1 downTo 2).hashCode(), (2 downTo 3).hashCode())
    assertEquals((1L downTo 2L).hashCode(), (2L downTo 3L).hashCode())
    assertEquals(('a' downTo 'b').hashCode(), ('c' downTo 'd').hashCode())

    assertEquals(("range".."progression").hashCode(), ("hashcode".."equals").hashCode())
}
