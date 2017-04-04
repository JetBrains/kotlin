import kotlin.test.*

fun box() {
    expect(listOf(), { intArrayOf().filterIndexed { i, v -> i > v } })
    expect(listOf(2, 5, 8), { intArrayOf(2, 4, 3, 5, 8).filterIndexed { index, value -> index % 2 == value % 2 } })
    expect(listOf<Long>(2, 5, 8), { longArrayOf(2, 4, 3, 5, 8).filterIndexed { index, value -> index % 2 == (value % 2).toInt() } })
    expect(listOf<Byte>(2, 5, 8), { byteArrayOf(2, 4, 3, 5, 8).filterIndexed { index, value -> index % 2 == (value % 2).toInt() } })
    expect(listOf('9', 'e', 'a'), { charArrayOf('9', 'e', 'd', 'a').filterIndexed { index, c -> c == 'a' || index < 2 } })
    expect(listOf("a", "c", "d"), { arrayOf("a", "b", "c", "d").filterIndexed { index, s -> s == "a" || index >= 2 } })
}
