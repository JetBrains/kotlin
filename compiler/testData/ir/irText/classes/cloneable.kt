class A : Cloneable

interface I : Cloneable

class C : I

class OC : I {
    override fun clone(): OC = OC()
}