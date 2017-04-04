import kotlin.test.*



fun box() {
    assertEquals("7a", 0x7a.toByte().toString(16))
    assertEquals("-80", Byte.MIN_VALUE.toString(radix = 16))
    assertEquals("3v", Byte.MAX_VALUE.toString(radix = 32))
    assertEquals("-40", Byte.MIN_VALUE.toString(radix = 32))

    assertFailsWith<IllegalArgumentException>("Expected to fail with radix 37") { 37.toByte().toString(radix = 37) }
    assertFailsWith<IllegalArgumentException>("Expected to fail with radix 1") { 1.toByte().toString(radix = 1) }
}
