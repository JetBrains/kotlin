import kotlin.test.*

fun box() {
    expect(listOf(3, 2, 1)) { intArrayOf(1, 2, 3).reversed() }
    expect(listOf<Byte>(3, 2, 1)) { byteArrayOf(1, 2, 3).reversed() }
    expect(listOf<Short>(3, 2, 1)) { shortArrayOf(1, 2, 3).reversed() }
    expect(listOf<Long>(3, 2, 1)) { longArrayOf(1, 2, 3).reversed() }
    expect(listOf(3F, 2F, 1F)) { floatArrayOf(1F, 2F, 3F).reversed() }
    expect(listOf(3.0, 2.0, 1.0)) { doubleArrayOf(1.0, 2.0, 3.0).reversed() }
    expect(listOf('3', '2', '1')) { charArrayOf('1', '2', '3').reversed() }
    expect(listOf(false, false, true)) { booleanArrayOf(true, false, false).reversed() }
    expect(listOf("3", "2", "1")) { arrayOf("1", "2", "3").reversed() }
}
