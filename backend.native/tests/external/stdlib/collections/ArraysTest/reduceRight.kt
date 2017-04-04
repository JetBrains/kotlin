import kotlin.test.*

fun box() {
    expect(2) { intArrayOf(1, 2, 3).reduceRight { a, b -> a - b } }
    expect(2.toLong()) { longArrayOf(1, 2, 3).reduceRight { a, b -> a - b } }
    expect(2F) { floatArrayOf(1F, 2F, 3F).reduceRight { a, b -> a - b } }
    expect(2.0) { doubleArrayOf(1.0, 2.0, 3.0).reduceRight { a, b -> a - b } }
    expect('3') { charArrayOf('1', '3', '2').reduceRight { a, b -> if (a > b) a else b } }
    expect(false) { booleanArrayOf(true, true, false).reduceRight { a, b -> a && b } }
    expect(true) { booleanArrayOf(true, true).reduceRight { a, b -> a && b } }
    expect(2.toByte()) { byteArrayOf(1, 2, 3).reduceRight { a, b -> (a - b).toByte() } }
    expect(2.toShort()) { shortArrayOf(1, 2, 3).reduceRight { a, b -> (a - b).toShort() } }

    assertFailsWith<UnsupportedOperationException> {
        intArrayOf().reduceRight { a, b -> a + b }
    }
}
