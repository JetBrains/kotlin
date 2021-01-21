// !LANGUAGE: +InlineClasses

// FILE: utils.kt

inline class UInt(val value: Int)

// FILE: test.kt

fun test(u1: UInt, u2: UInt) {
    val a = u1.value

    val b = u1.value.hashCode()
    val c = u1.value + u2.value
}

// @TestKt.class:
// 0 INVOKESTATIC UInt\$Erased.getValue
// 0 INVOKESTATIC UInt\$Erased.box
// 0 INVOKEVIRTUAL UInt.unbox

// 0 valueOf
// 0 intValue
