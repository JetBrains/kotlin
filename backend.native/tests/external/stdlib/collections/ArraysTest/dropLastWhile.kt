import kotlin.test.*

fun box() {
    expect(listOf(), { intArrayOf().dropLastWhile { it < 3 } })
    expect(listOf(), { intArrayOf(1).dropLastWhile { it < 3 } })
    expect(listOf(2, 3), { intArrayOf(2, 3, 1).dropLastWhile { it < 3 } })
    expect(listOf(3000000000000), { longArrayOf(3000000000000, 2000000000000).dropLastWhile { it < 3000000000000 } })
    expect(listOf(2.toByte(), 3.toByte()), { byteArrayOf(2, 3, 1).dropLastWhile { it < 3 } })
    expect(listOf(2.toShort(), 3.toShort()), { shortArrayOf(2, 3, 1).dropLastWhile { it < 3 } })
    expect(listOf(2f, 3f), { floatArrayOf(2f, 3f, 1f).dropLastWhile { it < 3 } })
    expect(listOf(2.0, 3.0), { doubleArrayOf(2.0, 3.0, 1.0).dropLastWhile { it < 3 } })
    expect(listOf(true, false), { booleanArrayOf(true, false, true).dropLastWhile { it } })
    expect(listOf('a', 'b'), { charArrayOf('a', 'b', 'a').dropLastWhile { it < 'b' } })
    expect(listOf("a", "b"), { arrayOf("a", "b", "a").dropLastWhile { it < "b" } })
}
