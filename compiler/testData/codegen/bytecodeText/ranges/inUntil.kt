// IGNORE_BACKEND: JVM_IR
fun testByte(a: Byte, x: Byte, y: Byte) = a in x until y

fun testShort(a: Short, x: Short, y: Short) = a in x until y

fun testInt(a: Int, x: Int, y: Int) = a in x until y

fun testLong(a: Long, x: Long, y: Long) = a in x until y

// 0 until
// 0 INVOKEVIRTUAL
