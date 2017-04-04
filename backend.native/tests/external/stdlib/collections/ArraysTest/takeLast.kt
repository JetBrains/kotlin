import kotlin.test.*

fun box() {
    expect(listOf(), { intArrayOf().takeLast(1) })
    expect(listOf(), { intArrayOf(1).takeLast(0) })
    expect(listOf(1), { intArrayOf(1).takeLast(1) })
    expect(listOf(3), { intArrayOf(2, 3).takeLast(1) })
    expect(listOf(2000000000000), { longArrayOf(3000000000000, 2000000000000).takeLast(1) })
    expect(listOf(3.toByte()), { byteArrayOf(2, 3).takeLast(1) })
    expect(listOf(3.toShort()), { shortArrayOf(2, 3).takeLast(1) })
    expect(listOf(3.0f), { floatArrayOf(2f, 3f).takeLast(1) })
    expect(listOf(3.0), { doubleArrayOf(2.0, 3.0).takeLast(1) })
    expect(listOf(false), { booleanArrayOf(true, false).takeLast(1) })
    expect(listOf('b'), { charArrayOf('a', 'b').takeLast(1) })
    expect(listOf("b"), { arrayOf("a", "b").takeLast(1) })
    assertFails {
        listOf(1).takeLast(-1)
    }
}
