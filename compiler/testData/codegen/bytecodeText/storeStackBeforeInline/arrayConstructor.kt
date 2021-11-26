// WITH_STDLIB

fun test(): Int = "123".indexOfAny(CharArray(1000) { '1' })

// JVM_TEMPLATES:
// 5 ALOAD
// 5 ASTORE
// 7 ILOAD
// 6 ISTORE

// JVM_IR_TEMPLATES:
// 3 ALOAD
// 3 ASTORE
// 4 ILOAD
// 3 ISTORE
