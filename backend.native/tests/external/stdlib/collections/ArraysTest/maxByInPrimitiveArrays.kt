import kotlin.test.*

fun box() {
    expect(null, { intArrayOf().maxBy { it } })
    expect(1, { intArrayOf(1).maxBy { it } })
    expect(2, { intArrayOf(2, 3).maxBy { -it } })
    expect(3000000000000, { longArrayOf(3000000000000, 2000000000000).maxBy { it + 1 } })
    expect(3, { byteArrayOf(1, 3, 2).maxBy { it * it } })
    expect(3, { shortArrayOf(3, 2).maxBy { "a" } })
    expect(3.0F, { floatArrayOf(3.0F, 2.0F).maxBy { it.toString() } })
    expect(3.0, { doubleArrayOf(2.0, 3.0).maxBy { it * it } })
}
