import kotlin.test.*

fun box() {
    expect(0) { arrayOf<Int>().sum() }
    expect(14) { arrayOf(2, 3, 9).sum() }
    expect(3.0) { arrayOf(1.0, 2.0).sum() }
    expect(200) { arrayOf<Byte>(100, 100).sum() }
    expect(50000) { arrayOf<Short>(20000, 30000).sum() }
    expect(3000000000000) { arrayOf<Long>(1000000000000, 2000000000000).sum() }
    expect(3.0F) { arrayOf<Float>(1.0F, 2.0F).sum() }
}
