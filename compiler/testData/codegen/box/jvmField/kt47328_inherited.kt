// !LANGUAGE: -ProhibitJvmFieldOnOverrideFromInterfaceInPrimaryConstructor
// TARGET_BACKEND: JVM
// WITH_STDLIB

interface A { val x: Int }

open class B(@JvmField override val x: Int): A

class BB(x: Int) : B(x)

class C<D: A>(@JvmField val d: D)

class E(c: C<BB>) { val ax = c.d.x }
// CHECK_BYTECODE_TEXT
// 1 GETFIELD BB\.x \: I

fun box(): String {
    val e = E(C(BB(42)))
    if (e.ax != 42)
        return "Failed: ${e.ax}"
    return "OK"
}
