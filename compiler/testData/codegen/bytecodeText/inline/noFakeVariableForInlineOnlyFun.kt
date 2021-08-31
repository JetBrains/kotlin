@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@kotlin.internal.InlineOnly
inline fun inlineOnlyFun(): Int {
    var unused = 0
    var used1 = 0
    var used2 = 0
    ++used2
    return 42 + used1
}

fun test() = inlineOnlyFun()

// JVM_TEMPLATES
// 1 LDC 0
// 5 ICONST_0
// 6 ISTORE

// JVM_IR_TEMPLATES
// 0 LDC 0
// 5 ICONST_0
// 5 ISTORE