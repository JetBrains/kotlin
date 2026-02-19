// LANGUAGE: +InlineClasses
// IGNORE_BACKEND_K1: ANY
// Since KT-76950, we have new implicit casts in testNZ which lead to different bytecode compared to K1

// FILE: Z.kt
inline class Z(val x: Int)

// FILE: test.kt
fun testZ(z: Z) = z.equals(z)
fun testZ(z: Z, a: Any?) = z.equals(a)
fun testNZ(z: Z?) = z?.equals(z)

// @TestKt.class:
// 0 INVOKESTATIC Z\$Erased\.equals
// 0 INVOKESTATIC Z\-Erased\.equals
// 2 INVOKESTATIC Z\.equals-impl0 \(II\)Z
// 1 INVOKESTATIC Z\.equals-impl \(ILjava/lang/Object;\)Z
// 0 INVOKEVIRTUAL Z.equals
// 2 INVOKEVIRTUAL Z.unbox-impl
