package runtime.collections.typed_array0

import kotlin.test.*

@Test fun runTest() {
    // Those tests assume little endian bit ordering.
    val array = ByteArray(42)
    array.setLongAt(5, 0x1234_5678_9abc_def0)

    expect(0xdef0.toInt()) { array.charAt(5).toInt() }
    expect(0x9abc.toShort()) { array.shortAt(7) }
    expect(0x1234_5678) { array.intAt(9) }
    println("OK")
}