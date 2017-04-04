import kotlin.test.*

fun box() {
    expect(1) { intArrayOf(1, 2, 3).reduceRightIndexed { index, a, b -> index + a - b } }
    expect(1.toLong()) { longArrayOf(1, 2, 3).reduceRightIndexed { index, a, b -> index + a - b } }
    expect(1F) { floatArrayOf(1F, 2F, 3F).reduceRightIndexed { index, a, b -> index + a - b } }
    expect(1.0) { doubleArrayOf(1.0, 2.0, 3.0).reduceRightIndexed { index, a, b -> index + a - b } }
    expect('2') { charArrayOf('1', '3', '2').reduceRightIndexed { index, a, b -> if (a > b && index == 0) a else b } }
    expect(true) { booleanArrayOf(true, true, false).reduceRightIndexed { index, a, b -> a && b || index == 1 } }
    expect(false) { booleanArrayOf(true, true).reduceRightIndexed { index, a, b -> a && b && index != 0 } }
    expect(1.toByte()) { byteArrayOf(3, 2, 1).reduceRightIndexed { index, a, b -> if (index != 1) (a - b).toByte() else a.toByte() } }
    expect(1.toShort()) { shortArrayOf(3, 2, 1).reduceRightIndexed { index, a, b -> if (index != 1) (a - b).toShort() else a.toShort() } }

    assertFailsWith<UnsupportedOperationException> {
        intArrayOf().reduceRightIndexed { index, a, b -> a + b }
    }
}
