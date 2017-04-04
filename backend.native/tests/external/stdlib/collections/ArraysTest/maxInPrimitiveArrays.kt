import kotlin.test.*

fun box() {
    expect(null, { intArrayOf().max() })
    expect(1, { intArrayOf(1).max() })
    expect(3, { intArrayOf(2, 3).max() })
    expect(3000000000000, { longArrayOf(3000000000000, 2000000000000).max() })
    expect(3, { byteArrayOf(1, 3, 2).max() })
    expect(3, { shortArrayOf(3, 2).max() })
    expect(3.0F, { floatArrayOf(3.0F, 2.0F).max() })
    expect(3.0, { doubleArrayOf(2.0, 3.0).max() })
    expect('b', { charArrayOf('a', 'b').max() })
}
