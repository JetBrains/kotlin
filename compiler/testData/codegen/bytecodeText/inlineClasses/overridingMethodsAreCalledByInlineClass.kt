// !LANGUAGE: +InlineClasses

// FILE: Z.kt
interface IFoo {
    fun foo()
}

inline class Z(val x: Int) : IFoo {
    override fun foo() {}
}

// FILE: test.kt
fun testZ(z: Z) = z.foo()

fun testNZ(z: Z?) = z?.foo()

// @TestKt.class:
// 0 INVOKESTATIC Z\$Erased\.foo
// 0 INVOKESTATIC Z\-Erased\.foo
// 2 INVOKESTATIC Z\.foo-impl \(I\)V