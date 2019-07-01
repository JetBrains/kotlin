// !LANGUAGE: +InlineClasses

// FILE: Z.kt
inline class Z(val x: Int)

// FILE: Test.kt
fun test(xs: List<Z>) = xs[0]

// @TestKt.class:
// 0 INVOKEVIRTUAL Z\$Erased\.unbox
// 0 INVOKEVIRTUAL Z\-Erased\.unbox
// 1 INVOKEVIRTUAL Z\.unbox-impl \(\)I