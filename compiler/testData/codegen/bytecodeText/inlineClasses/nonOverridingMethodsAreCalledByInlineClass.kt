// !LANGUAGE: +InlineClasses

inline class Z(val x: Int) {
    fun foo() {}
}

fun testZ(z: Z) = z.foo()
fun testNZ(z: Z?) = z?.foo()

// 0 INVOKESTATIC Z\$Erased\.foo
// 0 INVOKESTATIC Z\-Erased\.foo
// 2 INVOKESTATIC Z\.foo-impl \(I\)V
