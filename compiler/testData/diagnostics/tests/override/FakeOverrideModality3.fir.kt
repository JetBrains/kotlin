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
<!MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED!>class Z<!> : B, C, D
