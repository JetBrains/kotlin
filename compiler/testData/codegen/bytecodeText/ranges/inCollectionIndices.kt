// WITH_RUNTIME

fun testCollection(i: Int, xs: List<Any>) = i in xs.indices

fun testLongWithCollection(i: Long, xs: List<Any>) = i in xs.indices

// 0 INVOKESTATIC kotlin/collections/CollectionsKt.getIndices
// 0 INVOKEVIRTUAL kotlin/ranges/IntRange.contains
// 2 INVOKEINTERFACE java/util/Collection.size
// 2 I2L
