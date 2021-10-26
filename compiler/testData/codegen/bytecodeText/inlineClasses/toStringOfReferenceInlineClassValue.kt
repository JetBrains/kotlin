// !LANGUAGE: +InlineClasses
// Completely incorrect bytecode - see `box/inlineClasses/toStringOfUnboxedNullable.kt`
// IGNORE_BACKEND: JVM

// FILE: Z.kt
inline class Z(val x: Any)

// FILE: test.kt
fun testZ(z: Z) = z.toString()
fun testZT(z: Z) = "$z"
fun testNZ(z: Z?) = z?.toString() // `Z?` is unboxed into `Any?` even before the null check
fun testNZA(z: Z?) = z.toString() // so all of these call toString-impl
fun testNZT(z: Z?) = "$z"

// @TestKt.class:
// 5 INVOKESTATIC Z\.toString-impl \(Ljava/lang/Object;\)Ljava/lang/String;
// 0 INVOKESTATIC java/lang/String.valueOf
