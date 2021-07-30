@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@kotlin.internal.InlineOnly
inline fun inlineOnlyFun() = 42

fun test() = inlineOnlyFun()

// JVM_TEMPLATES
// 1 LDC 0
// 1 ICONST_0
// 0 ISTORE 1
// 2 ISTORE 0

// JVM_IR_TEMPLATES
// 0 LDC 0
// 0 ICONST_0
// 0 ISTORE 1
// 0 ISTORE 0