// WITH_RUNTIME

fun testCharSequence(i: Int, cs: CharSequence) = i in cs.indices

fun testLongWithCharSequence(i: Long, cs: CharSequence) = i in cs.indices

// 0 INVOKESTATIC kotlin/text/StringsKt.getIndices
// 0 INVOKEVIRTUAL kotlin/ranges/IntRange.contains
// 2 INVOKEINTERFACE java/lang/CharSequence.length
// 2 I2L

