class A : Cloneable

interface I : Cloneable

interface I2 : Cloneable {
    override fun clone(): Any
}

class C : I

class OC : I {
    override fun clone(): OC = OC()
}

abstract class ACC : Cloneable

abstract class ACI : I

abstract class ACI2 : I2