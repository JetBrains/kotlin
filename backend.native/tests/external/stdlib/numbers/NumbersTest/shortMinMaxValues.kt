import kotlin.test.*

object NumbersTestConstants {
    public const val byteMinSucc: Byte = (Byte.MIN_VALUE + 1).toByte()
    public const val byteMaxPred: Byte = (Byte.MAX_VALUE - 1).toByte()

    public const val shortMinSucc: Short = (Short.MIN_VALUE + 1).toShort()
    public const val shortMaxPred: Short = (Short.MAX_VALUE - 1).toShort()

    public const val intMinSucc: Int = Int.MIN_VALUE + 1
    public const val intMaxPred: Int = Int.MAX_VALUE - 1

    public const val longMinSucc: Long = Long.MIN_VALUE + 1L
    public const val longMaxPred: Long = Long.MAX_VALUE - 1L
}

var one: Int = 1
var oneS: Short = 1
var oneB: Byte = 1

fun box() {
    assertTrue(Short.MIN_VALUE < 0)
    assertTrue(Short.MAX_VALUE > 0)

    assertEquals(NumbersTestConstants.shortMinSucc, Short.MIN_VALUE.inc())
    assertEquals(NumbersTestConstants.shortMaxPred, Short.MAX_VALUE.dec())

    // overflow behavior
    expect(Short.MIN_VALUE) { (Short.MAX_VALUE + oneS).toShort() }
    expect(Short.MAX_VALUE) { (Short.MIN_VALUE - oneS).toShort() }
}
