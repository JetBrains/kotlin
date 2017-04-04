import kotlin.test.*

fun box() {
    expect(listOf(), { intArrayOf().dropWhile { it < 3 } })
    expect(listOf(), { intArrayOf(1).dropWhile { it < 3 } })
    expect(listOf(3, 1), { intArrayOf(2, 3, 1).dropWhile { it < 3 } })
    expect(listOf(2000000000000), { longArrayOf(3000000000000, 2000000000000).dropWhile { it > 2000000000000 } })
    expect(listOf(3.toByte(), 1.toByte()), { byteArrayOf(2, 3, 1).dropWhile { it < 3 } })
    expect(listOf(3.toShort(), 1.toShort()), { shortArrayOf(2, 3, 1).dropWhile { it < 3 } })
    expect(listOf(3f, 1f), { floatArrayOf(2f, 3f, 1f).dropWhile { it < 3 } })
    expect(listOf(3.0, 1.0), { doubleArrayOf(2.0, 3.0, 1.0).dropWhile { it < 3 } })
    expect(listOf(false, true), { booleanArrayOf(true, false, true).dropWhile { it } })
    expect(listOf('b', 'a'), { charArrayOf('a', 'b', 'a').dropWhile { it < 'b' } })
    expect(listOf("b", "a"), { arrayOf("a", "b", "a").dropWhile { it < "b" } })
}
