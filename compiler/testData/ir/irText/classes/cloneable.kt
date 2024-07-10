// FIR_IDENTICAL
// TARGET_BACKEND: JVM
// STATUS: Cloneable is JVM-specific API

class A : Cloneable

interface I : Cloneable

class C : I

class OC : I {
    override fun clone(): OC = OC()
}
