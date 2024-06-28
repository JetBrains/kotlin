package d

interface A {
    fun foo() = 1
}

interface B {
    fun foo() = 2
}

open <!CANNOT_INFER_VISIBILITY, MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED!>class C<!> : A, B {}

interface E {
    fun foo(): Int
}

<!CANNOT_INFER_VISIBILITY!>class D<!> : C() {}
