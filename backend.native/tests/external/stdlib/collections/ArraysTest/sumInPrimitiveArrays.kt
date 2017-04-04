import kotlin.test.*

fun box() {
    expect(0) { intArrayOf().sum() }
    expect(14) { intArrayOf(2, 3, 9).sum() }
    expect(3.0) { doubleArrayOf(1.0, 2.0).sum() }
    expect(200) { byteArrayOf(100, 100).sum() }
    expect(50000) { shortArrayOf(20000, 30000).sum() }
    expect(3000000000000) { longArrayOf(1000000000000, 2000000000000).sum() }
    expect(3.0F) { floatArrayOf(1.0F, 2.0F).sum() }
}
