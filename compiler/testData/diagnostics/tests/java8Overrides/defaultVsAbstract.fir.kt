interface ILeft {
    fun foo() {}
}

interface IRight {
    fun foo()
}

interface IDerived : ILeft, IRight

<!MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED!>class CDerived<!> : ILeft, IRight

abstract class ADerived : ILeft, IRight