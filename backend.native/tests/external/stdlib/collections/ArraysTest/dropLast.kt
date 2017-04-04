import kotlin.test.*

fun box() {
    expect(listOf(), { intArrayOf().dropLast(1) })
    expect(listOf(), { intArrayOf(1).dropLast(1) })
    expect(listOf(1), { intArrayOf(1).dropLast(0) })
    expect(listOf(2), { intArrayOf(2, 3).dropLast(1) })
    expect(listOf(3000000000000), { longArrayOf(3000000000000, 2000000000000).dropLast(1) })
    expect(listOf(2.toByte()), { byteArrayOf(2, 3).dropLast(1) })
    expect(listOf(2.toShort()), { shortArrayOf(2, 3).dropLast(1) })
    expect(listOf(2.0f), { floatArrayOf(2f, 3f).dropLast(1) })
    expect(listOf(2.0), { doubleArrayOf(2.0, 3.0).dropLast(1) })
    expect(listOf(true), { booleanArrayOf(true, false).dropLast(1) })
    expect(listOf('a'), { charArrayOf('a', 'b').dropLast(1) })
    expect(listOf("a"), { arrayOf("a", "b").dropLast(1) })
    assertFails {
        listOf(1).dropLast(-1)
    }
}
