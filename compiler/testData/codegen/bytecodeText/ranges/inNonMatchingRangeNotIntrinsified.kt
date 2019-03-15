fun byteInFloat(x: Byte, a: Float, b: Float) = x in a .. b
fun byteInDouble(x: Byte, a: Double, b: Double) = x in a .. b
fun shortInFloat(x: Short, a: Float, b: Float) = x in a .. b
fun shortInDouble(x: Short, a: Double, b: Double) = x in a .. b
fun intInFloat(x: Int, a: Float, b: Float) = x in a .. b
fun intInDouble(x: Int, a: Double, b: Double) = x in a .. b
fun longInFloat(x: Long, a: Float, b: Float) = x in a .. b
fun longInDouble(x: Long, a: Double, b: Double) = x in a .. b
fun floatInInt(x: Float, a: Int, b: Int) = x in a .. b
fun floatInLong(x: Float, a: Long, b: Long) = x in a .. b
fun doubleInInt(x: Double, a: Int, b: Int) = x in a .. b
fun doubleInLong(x: Double, a: Long, b: Long) = x in a .. b

// 4 INVOKESPECIAL
// 4 NEW
// 8 rangeTo
// 2 longRangeContains
// 2 intRangeContains
// 4 doubleRangeContains
// 4 floatRangeContains
