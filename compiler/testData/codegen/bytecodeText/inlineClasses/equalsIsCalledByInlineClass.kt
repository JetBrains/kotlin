// LANGUAGE: +InlineClasses

// FILE: Z.kt
inline class Z(val x: Int)

// FILE: test.kt
fun testZ(z: Z) = z.equals(z)
fun testZ(z: Z, a: Any?) = z.equals(a)
fun testNZ(z: Z?) = z?.equals(z)

// @TestKt.class:
// JVM_IR_TEMPLATES
// 0 INVOKESTATIC Z\$Erased\.equals
// 0 INVOKESTATIC Z\-Erased\.equals
// 1 INVOKESTATIC Z\.equals-impl0 \(II\)Z
// 1 INVOKESTATIC Z\.equals-impl \(ILjava/lang/Object;\)Z
// 1 INVOKEVIRTUAL Z.equals
// 0 INVOKEVIRTUAL Z.unbox-impl