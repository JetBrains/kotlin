// !LANGUAGE: -ProhibitJvmFieldOnOverrideFromInterfaceInPrimaryConstructor
// TARGET_BACKEND: JVM
// WITH_STDLIB

interface A { val x: Int }

class B(@JvmField override val x: Int): A

class C<D: A>(@JvmField val d: D)

class E(c: C<B>) { val ax = c.d.x }

fun box(): String {
    val e = E(C(B(42)))
    if (e.ax != 42)
        return "Failed: ${e.ax}"
    return "OK"
}
