// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

// FILE: utils.kt

inline class UInt(private val u: Int)

// FILE: test.kt

fun test(x: UInt?, y: UInt) {
    val a = x ?: y // unbox
    val b = x ?: x!! // unbox unbox
}

// @TestKt.class:
// 0 INVOKESTATIC UInt\$Erased.box
// 3 INVOKEVIRTUAL UInt.unbox

// 0 valueOf
// 0 intValue