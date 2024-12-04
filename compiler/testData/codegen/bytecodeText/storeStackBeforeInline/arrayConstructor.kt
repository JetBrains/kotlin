// WITH_STDLIB

fun test(): Int = "123".indexOfAny(CharArray(1000) { '1' })

// 3 ALOAD
// 3 ASTORE
// 3 ILOAD
// 2 ISTORE
