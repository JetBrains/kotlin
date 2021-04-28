// FIR_IDENTICAL
interface ILeft {
    fun foo() {}
}

interface IRight {
    fun foo()
}

<!MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED!>interface IDerived<!> : ILeft, IRight

<!MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED!>class CDerived<!> : ILeft, IRight

abstract <!MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED!>class ADerived<!> : ILeft, IRight