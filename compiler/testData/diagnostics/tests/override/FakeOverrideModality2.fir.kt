interface A {
    fun foo() {}
}

interface B : A {
    abstract override fun foo()
}

interface C : A {
    abstract override fun foo()
}

interface D : A {
    override fun foo() = super.foo()
}

// Fake override Z#foo should be open
class Z : B, C, D
