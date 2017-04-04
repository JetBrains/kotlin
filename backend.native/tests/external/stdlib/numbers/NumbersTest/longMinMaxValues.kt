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
    assertTrue(Long.MIN_VALUE < 0)
    assertTrue(Long.MAX_VALUE > 0)

    assertEquals(NumbersTestConstants.longMinSucc, Long.MIN_VALUE + one)
    assertEquals(NumbersTestConstants.longMaxPred, Long.MAX_VALUE - one)

    // overflow behavior
    expect(Long.MIN_VALUE) { Long.MAX_VALUE + one }
    expect(Long.MAX_VALUE) { Long.MIN_VALUE - one }
}
