// !LANGUAGE: +InlineClasses

// FILE: Z.kt
inline class Z(val x: Int)

// FILE: test.kt
fun testZ(z: Z) = z.toString()
fun testNZ(z: Z?) = z?.toString()

// @TestKt.class:
// 0 INVOKESTATIC Z\$Erased\.toString
// 0 INVOKESTATIC Z\-Erased\.toString
// 2 INVOKESTATIC Z\.toString-impl \(I\)Ljava/lang/String;
