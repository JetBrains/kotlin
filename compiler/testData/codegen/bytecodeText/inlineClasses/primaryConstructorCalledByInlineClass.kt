// !LANGUAGE: +InlineClasses

// FILE: Z.kt
inline class Z(val x: Int)

// FILE: test.kt
fun testZ() = Z(42)

// @TestKt.class:
// 1 INVOKESTATIC Z\.constructor-impl \(I\)I
