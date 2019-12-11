interface A {
    fun foo() {}
}

interface B : A {
    abstract override fun foo()
}

interface C : A {
    abstract override fun foo()
}

interface D : A

// Fake override Z#foo should be abstract
class Z : B, C, D
