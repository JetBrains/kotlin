// !LANGUAGE: +InlineClasses

// FILE: Z.kt
inline class Z(val x: Int)

// FILE: Test.kt
fun test(): Any = Z(42)

// @TestKt.class:
// 0 INVOKESTATIC Z\$Erased\.box
// 0 INVOKESTATIC Z\-Erased\.box
// 1 INVOKESTATIC Z\.box-impl \(I\)LZ;