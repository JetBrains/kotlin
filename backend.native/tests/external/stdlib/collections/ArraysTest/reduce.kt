import kotlin.test.*

fun box() {
    expect(-4) { intArrayOf(1, 2, 3).reduce { a, b -> a - b } }
    expect(-4.toLong()) { longArrayOf(1, 2, 3).reduce { a, b -> a - b } }
    expect(-4F) { floatArrayOf(1F, 2F, 3F).reduce { a, b -> a - b } }
    expect(-4.0) { doubleArrayOf(1.0, 2.0, 3.0).reduce { a, b -> a - b } }
    expect('3') { charArrayOf('1', '3', '2').reduce { a, b -> if (a > b) a else b } }
    expect(false) { booleanArrayOf(true, true, false).reduce { a, b -> a && b } }
    expect(true) { booleanArrayOf(true, true).reduce { a, b -> a && b } }
    expect(0.toByte()) { byteArrayOf(3, 2, 1).reduce { a, b -> (a - b).toByte() } }
    expect(0.toShort()) { shortArrayOf(3, 2, 1).reduce { a, b -> (a - b).toShort() } }

    assertFailsWith<UnsupportedOperationException> {
        intArrayOf().reduce { a, b -> a + b }
    }
}
