// IGNORE_BACKEND: JVM_IR
fun testPrimitiveArray(i: Int, ints: IntArray) = i in ints.indices

fun testObjectArray(i: Int, xs: Array<Any>) = i in xs.indices

fun testLongWithPrimitiveArray(i: Long, ints: IntArray) = i in ints.indices

fun testLongWithObjectArray(i: Long, xs: Array<Any>) = i in xs.indices

// 0 getIndices
// 0 contains
// 2 I2L
// 4 ARRAYLENGTH
// 2 LCONST_0
// 6 ICONST_0
