abstract class ALeft {
    abstract fun foo()
}

interface IRight {
    fun foo() {}
}

class CDerived : ALeft(), IRight