// LANGUAGE: +JsIntegerDivisionCheck

fun intDivByZero(a: Int, b: Int): Int = a / b
fun intRemByZero(a: Int, b: Int): Int = a % b
fun byteDivByZero(a: Byte, b: Byte): Int = a / b
fun byteRemByZero(a: Byte, b: Byte): Int = a % b
fun shortDivByZero(a: Short, b: Short): Int = a / b
fun shortRemByZero(a: Short, b: Short): Int = a % b
fun longDivByZero(a: Long, b: Long): Long = a / b
fun longRemByZero(a: Long, b: Long): Long = a % b

fun uintDivByZero(a: UInt, b: UInt): UInt = a / b
fun uintRemByZero(a: UInt, b: UInt): UInt = a % b
fun ubyteDivByZero(a: UByte, b: UByte): UInt = a / b
fun ubyteRemByZero(a: UByte, b: UByte): UInt = a % b
fun ushortDivByZero(a: UShort, b: UShort): UInt = a / b
fun ushortRemByZero(a: UShort, b: UShort): UInt = a % b
fun ulongDivByZero(a: ULong, b: ULong): ULong = a / b
fun ulongRemByZero(a: ULong, b: ULong): ULong = a % b

fun floatDivByZero(a: Float, b: Float): Float = a / b
fun floatRemByZero(a: Float, b: Float): Float = a % b
fun doubleDivByZero(a: Double, b: Double): Double = a / b
fun doubleRemByZero(a: Double, b: Double): Double = a % b

fun assertThrowsArithmeticException(block: () -> Unit): Boolean {
    try {
        block()
        return false
    } catch (e: ArithmeticException) {
        return true
    }
}

fun box(): String {
    if (!assertThrowsArithmeticException { intDivByZero(-1, 0) }) return "fail: Int div by zero should throw ArithmeticException"
    if (!assertThrowsArithmeticException { intDivByZero(0, 0) }) return "fail: Int div by zero should throw ArithmeticException"
    if (!assertThrowsArithmeticException { intDivByZero(1, 0) }) return "fail: Int div by zero should throw ArithmeticException"
    if (!assertThrowsArithmeticException { intRemByZero(-1, 0) }) return "fail: Int rem by zero should throw ArithmeticException"
    if (!assertThrowsArithmeticException { intRemByZero(0, 0) }) return "fail: Int rem by zero should throw ArithmeticException"
    if (!assertThrowsArithmeticException { intRemByZero(1, 0) }) return "fail: Int rem by zero should throw ArithmeticException"
    if (!assertThrowsArithmeticException { byteDivByZero(-1, 0) }) return "fail: Byte div by zero should throw ArithmeticException"
    if (!assertThrowsArithmeticException { byteDivByZero(0, 0) }) return "fail: Byte div by zero should throw ArithmeticException"
    if (!assertThrowsArithmeticException { byteDivByZero(1, 0) }) return "fail: Byte div by zero should throw ArithmeticException"
    if (!assertThrowsArithmeticException { byteRemByZero(-1, 0) }) return "fail: Byte rem by zero should throw ArithmeticException"
    if (!assertThrowsArithmeticException { byteRemByZero(0, 0) }) return "fail: Byte rem by zero should throw ArithmeticException"
    if (!assertThrowsArithmeticException { byteRemByZero(1, 0) }) return "fail: Byte rem by zero should throw ArithmeticException"
    if (!assertThrowsArithmeticException { shortDivByZero(-1, 0) }) return "fail: Short div by zero should throw ArithmeticException"
    if (!assertThrowsArithmeticException { shortDivByZero(0, 0) }) return "fail: Short div by zero should throw ArithmeticException"
    if (!assertThrowsArithmeticException { shortDivByZero(1, 0) }) return "fail: Short div by zero should throw ArithmeticException"
    if (!assertThrowsArithmeticException { shortRemByZero(-1, 0) }) return "fail: Short rem by zero should throw ArithmeticException"
    if (!assertThrowsArithmeticException { shortRemByZero(0, 0) }) return "fail: Short rem by zero should throw ArithmeticException"
    if (!assertThrowsArithmeticException { shortRemByZero(1, 0) }) return "fail: Short rem by zero should throw ArithmeticException"
    if (!assertThrowsArithmeticException { longDivByZero(-1L, 0L) }) return "fail: Long div by zero should throw ArithmeticException"
    if (!assertThrowsArithmeticException { longDivByZero(0L, 0L) }) return "fail: Long div by zero should throw ArithmeticException"
    if (!assertThrowsArithmeticException { longDivByZero(1L, 0L) }) return "fail: Long div by zero should throw ArithmeticException"
    if (!assertThrowsArithmeticException { longRemByZero(-1L, 0L) }) return "fail: Long rem by zero should throw ArithmeticException"
    if (!assertThrowsArithmeticException { longRemByZero(0L, 0L) }) return "fail: Long rem by zero should throw ArithmeticException"
    if (!assertThrowsArithmeticException { longRemByZero(1L, 0L) }) return "fail: Long rem by zero should throw ArithmeticException"

    if (!assertThrowsArithmeticException { uintDivByZero(0u, 0u) }) return "fail: UInt div by zero should throw ArithmeticException"
    if (!assertThrowsArithmeticException { uintDivByZero(1u, 0u) }) return "fail: UInt div by zero should throw ArithmeticException"
    if (!assertThrowsArithmeticException { uintRemByZero(0u, 0u) }) return "fail: UInt rem by zero should throw ArithmeticException"
    if (!assertThrowsArithmeticException { uintRemByZero(1u, 0u) }) return "fail: UInt rem by zero should throw ArithmeticException"
    if (!assertThrowsArithmeticException { ubyteDivByZero(0u, 0u) }) return "fail: UByte div by zero should throw ArithmeticException"
    if (!assertThrowsArithmeticException { ubyteDivByZero(1u, 0u) }) return "fail: UByte div by zero should throw ArithmeticException"
    if (!assertThrowsArithmeticException { ubyteRemByZero(0u, 0u) }) return "fail: UByte rem by zero should throw ArithmeticException"
    if (!assertThrowsArithmeticException { ubyteRemByZero(1u, 0u) }) return "fail: UByte rem by zero should throw ArithmeticException"
    if (!assertThrowsArithmeticException { ushortDivByZero(0u, 0u) }) return "fail: UShort div by zero should throw ArithmeticException"
    if (!assertThrowsArithmeticException { ushortDivByZero(1u, 0u) }) return "fail: UShort div by zero should throw ArithmeticException"
    if (!assertThrowsArithmeticException { ushortRemByZero(0u, 0u) }) return "fail: UShort rem by zero should throw ArithmeticException"
    if (!assertThrowsArithmeticException { ushortRemByZero(1u, 0u) }) return "fail: UShort rem by zero should throw ArithmeticException"
    if (!assertThrowsArithmeticException { ulongDivByZero(0UL, 0UL) }) return "fail: ULong div by zero should throw ArithmeticException"
    if (!assertThrowsArithmeticException { ulongDivByZero(1UL, 0UL) }) return "fail: ULong div by zero should throw ArithmeticException"
    if (!assertThrowsArithmeticException { ulongRemByZero(0UL, 0UL) }) return "fail: ULong rem by zero should throw ArithmeticException"
    if (!assertThrowsArithmeticException { ulongRemByZero(1UL, 0UL) }) return "fail: ULong rem by zero should throw ArithmeticException"


    if (floatDivByZero(-1f, 0f) != Float.NEGATIVE_INFINITY) return "fail: Float div of negative numbers by zero should equals to Float.NEGATIVE_INFINITY"
    if (!floatDivByZero(0f, 0f).isNaN()) return "fail: Float div zero by zero should equals to Float.NaN"
    if (floatDivByZero(1f, 0f) != Float.POSITIVE_INFINITY) return "fail: Float div by zero should equals to Float.POSITIVE_INFINITY"
    if (!floatRemByZero(-1f, 0f).isNaN()) return "fail: Float div by zero should be Float.NaN"
    if (!floatRemByZero(0f, 0f).isNaN()) return "fail: Float div by zero should be Float.NaN"
    if (!floatRemByZero(1f, 0f).isNaN()) return "fail: Float div by zero should be Float.NaN"
    if (doubleDivByZero(-1.0, 0.0) != Double.NEGATIVE_INFINITY) return "fail: Double div of negative numbers by zero should equals to Double.NEGATIVE_INFINITY"
    if (!doubleDivByZero(0.0, 0.0).isNaN()) return "fail: Double div zero by zero should equals to Double.NaN"
    if (doubleDivByZero(1.0, 0.0) != Double.POSITIVE_INFINITY) return "fail: Double div by zero should equals to Double.POSITIVE_INFINITY"
    if (!doubleRemByZero(-1.0, 0.0).isNaN()) return "fail: Double div by zero should be Double.NaN"
    if (!doubleRemByZero(0.0, 0.0).isNaN()) return "fail: Double div by zero should be Double.NaN"
    if (!doubleRemByZero(1.0, 0.0).isNaN()) return "fail: Double div by zero should be Double.NaN"

    return "OK"
}
