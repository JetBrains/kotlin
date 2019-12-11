interface ILeft {
    fun foo() {}
}

interface IRight {
    fun foo()
}

interface IDerived : ILeft, IRight

class CDerived : ILeft, IRight

abstract class ADerived : ILeft, IRight