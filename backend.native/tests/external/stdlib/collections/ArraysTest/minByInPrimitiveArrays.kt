import kotlin.test.*

fun box() {
    expect(null, { intArrayOf().minBy { it } })
    expect(1, { intArrayOf(1).minBy { it } })
    expect(3, { intArrayOf(2, 3).minBy { -it } })
    expect(2000000000000, { longArrayOf(3000000000000, 2000000000000).minBy { it + 1 } })
    expect(1, { byteArrayOf(1, 3, 2).minBy { it * it } })
    expect(3, { shortArrayOf(3, 2).minBy { "a" } })
    expect(2.0F, { floatArrayOf(3.0F, 2.0F).minBy { it.toString() } })
    expect(2.0, { doubleArrayOf(2.0, 3.0).minBy { it * it } })
}
