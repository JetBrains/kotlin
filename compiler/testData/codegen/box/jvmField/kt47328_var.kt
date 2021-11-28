// !LANGUAGE: -ProhibitJvmFieldOnOverrideFromInterfaceInPrimaryConstructor
// TARGET_BACKEND: JVM
// WITH_STDLIB

interface A { var x: Int }

class B(@JvmField override var x: Int): A

class C<D: A>(@JvmField val d: D)

class E(c: C<B>) {
    init {
        c.d.x = 42
    }
    val ax = c.d.x
}

fun box(): String {
    val e = E(C(B(1234)))
    if (e.ax != 42)
        return "Failed: ${e.ax}"
    return "OK"
}
