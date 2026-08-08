// OPT_IN: kotlin.ExperimentalStdlibApi

fun testChar(a: Char, x: Char, y: Char) = a in x..<y

fun testByte(a: Byte, x: Byte, y: Byte) = a in x..<y

fun testShort(a: Short, x: Short, y: Short) = a in x..<y

fun testInt(a: Int, x: Int, y: Int) = a in x..<y

fun testLong(a: Long, x: Long, y: Long) = a in x..<y

fun testFloat(a: Float, x: Float, y: Float) = a in x..<y

fun testDouble(a: Double, x: Double, y: Double) = a in x..<y

fun testFloatInDoubleRange(a: Float, x: Double, y: Double) = a in x..<y

fun testDoubleLiteral(a: Double) = a in 0.0..<2.0

// 0 until
// 0 rangeUntil
// 0 doubleRangeContains
// 0 floatRangeContains
// 0 INVOKEVIRTUAL
// 0 contains
// 0 valueOf
