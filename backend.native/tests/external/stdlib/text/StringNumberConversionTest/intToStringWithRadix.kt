import kotlin.test.*


fun box() {
    assertEquals("-ff", (-255).toString(radix = 16))
    assertEquals("1100110", 102.toString(radix = 2))
    assertEquals("kona", 411787.toString(radix = 27))
    assertFailsWith<IllegalArgumentException>("Expected to fail with radix 37") { 37.toString(radix = 37) }
    assertFailsWith<IllegalArgumentException>("Expected to fail with radix 1") { 1.toString(radix = 1) }

}
