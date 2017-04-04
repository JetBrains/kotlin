import kotlin.test.*

fun box() {
    expect(listOf(), { intArrayOf().take(1) })
    expect(listOf(), { intArrayOf(1).take(0) })
    expect(listOf(1), { intArrayOf(1).take(1) })
    expect(listOf(2), { intArrayOf(2, 3).take(1) })
    expect(listOf(3000000000000), { longArrayOf(3000000000000, 2000000000000).take(1) })
    expect(listOf(2.toByte()), { byteArrayOf(2, 3).take(1) })
    expect(listOf(2.toShort()), { shortArrayOf(2, 3).take(1) })
    expect(listOf(2.0f), { floatArrayOf(2f, 3f).take(1) })
    expect(listOf(2.0), { doubleArrayOf(2.0, 3.0).take(1) })
    expect(listOf(true), { booleanArrayOf(true, false).take(1) })
    expect(listOf('a'), { charArrayOf('a', 'b').take(1) })
    expect(listOf("a"), { arrayOf("a", "b").take(1) })
    assertFails {
        listOf(1).take(-1)
    }
}
