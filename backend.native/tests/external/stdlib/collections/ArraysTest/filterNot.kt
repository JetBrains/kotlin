import kotlin.test.*

fun box() {
    expect(listOf(), { intArrayOf().filterNot { it > 2 } })
    expect(listOf(1), { intArrayOf(1).filterNot { it > 2 } })
    expect(listOf(2), { intArrayOf(2, 3).filterNot { it > 2 } })
    expect(listOf(2000000000000), { longArrayOf(3000000000000, 2000000000000).filterNot { it > 2000000000000 } })
    expect(listOf(2.toByte()), { byteArrayOf(2, 3).filterNot { it > 2 } })
    expect(listOf(2.toShort()), { shortArrayOf(2, 3).filterNot { it > 2 } })
    expect(listOf(2.0f), { floatArrayOf(2f, 3f).filterNot { it > 2 } })
    expect(listOf(2.0), { doubleArrayOf(2.0, 3.0).filterNot { it > 2 } })
    expect(listOf(false), { booleanArrayOf(true, false).filterNot { it } })
    expect(listOf('a'), { charArrayOf('a', 'b').filterNot { it > 'a' } })
    expect(listOf("a"), { arrayOf("a", "b").filterNot { it > "a" } })
}
