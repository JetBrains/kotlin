// TARGET_BACKEND: JVM
// STATUS: Cloneable is JVM-specific API
// IGNORE_BACKEND: JKLIB
// ^KT-86348 java.lang.AssertionError: Can't find built-in class kotlin.Cloneable

class A : Cloneable

interface I : Cloneable

class C : I

class OC : I {
    override fun clone(): OC = OC()
}
