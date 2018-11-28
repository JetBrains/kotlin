// !LANGUAGE: +InlineClasses

// FILE: utils.kt

inline class UInt(private val data: Int)

// FILE: test.kt

fun f() {
    val unull = UInt(1) ?: null
}

// @TestKt.class:
// 0 INVOKESTATIC UInt\$Erased.box
// 0 INVOKESTATIC UInt\.box
// 0 INVOKEVIRTUAL UInt.unbox
// 0 valueOf
// 0 intValue