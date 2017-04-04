import kotlin.test.*

fun box() {
    expect(null, { intArrayOf().min() })
    expect(1, { intArrayOf(1).min() })
    expect(2, { intArrayOf(2, 3).min() })
    expect(2000000000000, { longArrayOf(3000000000000, 2000000000000).min() })
    expect(1, { byteArrayOf(1, 3, 2).min() })
    expect(2, { shortArrayOf(3, 2).min() })
    expect(2.0F, { floatArrayOf(3.0F, 2.0F).min() })
    expect(2.0, { doubleArrayOf(2.0, 3.0).min() })
    expect('a', { charArrayOf('a', 'b').min() })
}
