// !LANGUAGE: +InlineClasses

// FILE: Z.kt
inline class Z(val x: Int)

// FILE: test.kt
fun testZ(z: Z) = z.equals(z)
fun testZ(z: Z, a: Any?) = z.equals(a)
fun testNZ(z: Z?) = z?.equals(z)

// @TestKt.class:
// 0 INVOKESTATIC Z\$Erased\.equals
// 0 INVOKESTATIC Z\-Erased\.equals
// 3 INVOKESTATIC Z\.equals-impl \(ILjava/lang/Object;\)Z