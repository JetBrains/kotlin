import kotlin.test.*

fun box() {
    expect(listOf(), { intArrayOf().takeWhile { it < 3 } })
    expect(listOf(1), { intArrayOf(1).takeWhile { it < 3 } })
    expect(listOf(2), { intArrayOf(2, 3, 1).takeWhile { it < 3 } })
    expect(listOf(3000000000000), { longArrayOf(3000000000000, 2000000000000).takeWhile { it > 2000000000000 } })
    expect(listOf(2.toByte()), { byteArrayOf(2, 3, 1).takeWhile { it < 3 } })
    expect(listOf(2.toShort()), { shortArrayOf(2, 3, 1).takeWhile { it < 3 } })
    expect(listOf(2f), { floatArrayOf(2f, 3f, 1f).takeWhile { it < 3 } })
    expect(listOf(2.0), { doubleArrayOf(2.0, 3.0, 1.0).takeWhile { it < 3 } })
    expect(listOf(true), { booleanArrayOf(true, false, true).takeWhile { it } })
    expect(listOf('a'), { charArrayOf('a', 'c', 'b').takeWhile { it < 'c' } })
    expect(listOf("a"), { arrayOf("a", "c", "b").takeWhile { it < "c" } })
}
