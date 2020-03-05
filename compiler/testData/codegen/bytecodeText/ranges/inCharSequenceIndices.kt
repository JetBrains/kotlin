// IGNORE_BACKEND: JVM_IR
// TODO KT-36829 Optimize 'in' expressions in JVM_IR
fun testCharSequence(i: Int, cs: CharSequence) = i in cs.indices

fun testLongWithCharSequence(i: Long, cs: CharSequence) = i in cs.indices

// 0 getIndices
// 0 contains
// 2 length
// 1 I2L

