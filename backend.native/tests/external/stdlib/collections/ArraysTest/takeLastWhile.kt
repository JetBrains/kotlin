import kotlin.test.*

fun box() {
    expect(listOf(), { intArrayOf().takeLastWhile { it < 3 } })
    expect(listOf(1), { intArrayOf(1).takeLastWhile { it < 3 } })
    expect(listOf(1), { intArrayOf(2, 3, 1).takeLastWhile { it < 3 } })
    expect(listOf(2000000000000), { longArrayOf(3000000000000, 2000000000000).takeLastWhile { it < 3000000000000 } })
    expect(listOf(1.toByte()), { byteArrayOf(2, 3, 1).takeLastWhile { it < 3 } })
    expect(listOf(1.toShort()), { shortArrayOf(2, 3, 1).takeLastWhile { it < 3 } })
    expect(listOf(1f), { floatArrayOf(2f, 3f, 1f).takeLastWhile { it < 3 } })
    expect(listOf(1.0), { doubleArrayOf(2.0, 3.0, 1.0).takeLastWhile { it < 3 } })
    expect(listOf(true), { booleanArrayOf(true, false, true).takeLastWhile { it } })
    expect(listOf('b'), { charArrayOf('a', 'c', 'b').takeLastWhile { it < 'c' } })
    expect(listOf("b"), { arrayOf("a", "c", "b").takeLastWhile { it < "c" } })
}
