interface A {
    fun foo() {}
}

interface B : A {
    abstract override fun foo()
}

interface C {
    abstract fun foo()
}

// Fake override Z#foo should be abstract
<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class Z<!> : B, C
