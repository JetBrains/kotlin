// IGNORE_BACKEND: JVM_IR
fun testCollection(i: Int, xs: List<Any>) = i in xs.indices

fun testLongWithCollection(i: Long, xs: List<Any>) = i in xs.indices

// 0 getIndices
// 0 contains
// 2 size
// 1 I2L
