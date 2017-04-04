import kotlin.test.*

fun box() {
    expect(listOf(1), { intArrayOf(1).drop(0) })
    expect(listOf(), { intArrayOf().drop(1) })
    expect(listOf(), { intArrayOf(1).drop(1) })
    expect(listOf(3), { intArrayOf(2, 3).drop(1) })
    expect(listOf(2000000000000), { longArrayOf(3000000000000, 2000000000000).drop(1) })
    expect(listOf(3.toByte()), { byteArrayOf(2, 3).drop(1) })
    expect(listOf(3.toShort()), { shortArrayOf(2, 3).drop(1) })
    expect(listOf(3.0f), { floatArrayOf(2f, 3f).drop(1) })
    expect(listOf(3.0), { doubleArrayOf(2.0, 3.0).drop(1) })
    expect(listOf(false), { booleanArrayOf(true, false).drop(1) })
    expect(listOf('b'), { charArrayOf('a', 'b').drop(1) })
    expect(listOf("b"), { arrayOf("a", "b").drop(1) })
    assertFails {
        listOf(2).drop(-1)
    }
}
