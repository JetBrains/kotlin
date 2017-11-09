interface IBase {
    override fun toString(): String
}

interface IDerived : IBase

object BaseImpl : IBase {
    override fun toString(): String = "A"
}

object DerivedImpl : IDerived {
    override fun toString(): String = "A"
}

class Test1 : IBase by BaseImpl

class Test2 : IDerived by DerivedImpl
