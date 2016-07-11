interface A {
    fun test() {
    }
}

interface B : A {
    override fun test()
}

interface C : A

interface D : C, B

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class K<!> : D
