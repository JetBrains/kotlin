// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

// FILE: utils.kt

inline class UInt(val u: Int)

// FILE: test.kt

fun test(a1: Any, a2: UInt?, a3: Any?, a4: Any?) {
    val b1 = a1 as UInt // checkcast, unbox
    val b2 = a2 as UInt // unbox
    val b3 = a3 as UInt? // checkcast
    val b4 = a4 as? UInt // instanceof, checkcast
}

// @TestKt.class:
// 3 CHECKCAST UInt
// 2 INVOKEVIRTUAL UInt.unbox

// 1 INSTANCEOF UInt

// 0 intValue