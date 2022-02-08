// FIR_IDENTICAL
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
<!MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED!>class Z<!> : B, C, D
