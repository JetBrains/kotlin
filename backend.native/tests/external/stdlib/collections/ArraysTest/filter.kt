import kotlin.test.*

fun box() {
    expect(listOf(), { intArrayOf().filter { it > 2 } })
    expect(listOf(), { intArrayOf(1).filter { it > 2 } })
    expect(listOf(3), { intArrayOf(2, 3).filter { it > 2 } })
    expect(listOf(3000000000000), { longArrayOf(3000000000000, 2000000000000).filter { it > 2000000000000 } })
    expect(listOf(3.toByte()), { byteArrayOf(2, 3).filter { it > 2 } })
    expect(listOf(3.toShort()), { shortArrayOf(2, 3).filter { it > 2 } })
    expect(listOf(3.0f), { floatArrayOf(2f, 3f).filter { it > 2 } })
    expect(listOf(3.0), { doubleArrayOf(2.0, 3.0).filter { it > 2 } })
    expect(listOf(true), { booleanArrayOf(true, false).filter { it } })
    expect(listOf('b'), { charArrayOf('a', 'b').filter { it > 'a' } })
    expect(listOf("b"), { arrayOf("a", "b").filter { it > "a" } })
}
