// WITH_RUNTIME

fun testPrimitiveArray(i: Int, ints: IntArray) = i in ints.indices

fun testObjectArray(i: Int, xs: Array<Any>) = i in xs.indices

// 0 INVOKESTATIC kotlin/collections/ArraysKt.getIndices
// 0 INVOKEVIRTUAL kotlin/ranges/IntRange.contains
// 2 ARRAYLENGTH
