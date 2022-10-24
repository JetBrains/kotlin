// FIR_IDENTICAL
interface A {
    fun f() { }
}

interface B {
    fun f() { }
}

abstract class C : A {
    abstract override fun f()
}

class D : C(), A, B {
    override fun f() {
        super.f() // Resolves to super<B>.f call, but should be error
    }
}
