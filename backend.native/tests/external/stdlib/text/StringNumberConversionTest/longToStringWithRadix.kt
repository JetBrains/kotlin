import kotlin.test.*



fun box() {
    assertEquals("7f11223344556677", 0x7F11223344556677.toString(radix = 16))
    assertEquals("hazelnut", 1356099454469L.toString(radix = 36))
    assertEquals("-8000000000000000", Long.MIN_VALUE.toString(radix = 16))

    assertFailsWith<IllegalArgumentException>("Expected to fail with radix 37") { 37L.toString(radix = 37) }
    assertFailsWith<IllegalArgumentException>("Expected to fail with radix 1") { 1L.toString(radix = 1) }
}
