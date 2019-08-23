interface A {
    fun foo() {}
}

interface B {
    fun foo() {}
    fun bar() {}
}

class C : A, B {
    override fun bar() {
//            fun (B).bar(): Unit
//            │
        super.bar()
    }

    override fun foo() {
//               fun (A).foo(): Unit
//               │
        super<A>.foo()
//               fun (B).foo(): Unit
//               │
        super<B>.foo()
    }
}
