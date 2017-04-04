import kotlin.test.*



fun box() {
    assertEquals("7FFF", 0x7FFF.toShort().toString(radix = 16).toUpperCase())
    assertEquals("-8000", (-0x8000).toShort().toString(radix = 16))
    assertEquals("-sfs", (-29180).toShort().toString(radix = 32))

    assertFailsWith<IllegalArgumentException>("Expected to fail with radix 37") { 37.toShort().toString(radix = 37) }
    assertFailsWith<IllegalArgumentException>("Expected to fail with radix 1") { 1.toShort().toString(radix = 1) }
}
