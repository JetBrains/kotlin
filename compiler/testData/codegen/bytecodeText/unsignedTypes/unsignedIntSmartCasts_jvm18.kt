// JVM_TARGET: 1.8
// WITH_STDLIB

fun both(a: Any?, b: Any?) = if (a is UInt && b is UInt) a < b else null!!

// 1 compareUnsigned
// 0 uintCompare