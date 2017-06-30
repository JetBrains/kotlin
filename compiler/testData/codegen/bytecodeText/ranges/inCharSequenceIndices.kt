// WITH_RUNTIME

fun testCharSequence(i: Int, cs: CharSequence) = i in cs.indices

// 0 INVOKESTATIC kotlin/text/StringsKt.getIndices
// 0 INVOKEVIRTUAL kotlin/ranges/IntRange.contains
// 1 INVOKEINTERFACE java/lang/CharSequence.length

