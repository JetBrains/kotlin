// !LANGUAGE: -AbstractClassMemberNotImplementedWithIntermediateAbstractClass

abstract class ALeft {
    abstract fun foo()
}

interface IRight {
    fun foo() {}
}

class CDerived : ALeft(), IRight

abstract class CAbstract : ALeft(), IRight

class CDerivedFromAbstract : CAbstract()

interface ILeft {
    fun foo()
}

abstract class AILeft : ILeft

// Should be ERROR
class AILeftImpl : AILeft(), IRight

// Should be ERROR
class RightLeft : ILeft, IRight

interface IBase {
    fun foo()
}

interface IBaseEx : IBase {
    override fun foo() {}
}

abstract class AIBase : IBase

abstract class AIIntermediate : AIBase(), IBaseEx

class Impl : AIIntermediate()