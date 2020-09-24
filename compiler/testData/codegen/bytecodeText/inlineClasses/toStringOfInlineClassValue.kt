// !LANGUAGE: +InlineClasses

// FILE: Z.kt
inline class Z(val x: Int)

// FILE: test.kt
fun testZ(z: Z) = z.toString()
fun testZT(z: Z) = "$z"
fun testNZ(z: Z?) = z?.toString() // unboxed into Int after the null check
fun testNZA(z: Z?) = z.toString() // calls Any?.toString() on boxed value
fun testNZT(z: Z?) = "$z" // same

// @TestKt.class:
// 3 INVOKESTATIC Z\.toString-impl \(I\)Ljava/lang/String;
// 2 INVOKESTATIC java/lang/String.valueOf
