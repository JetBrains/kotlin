// !LANGUAGE: +AbstractClassMemberNotImplementedWithIntermediateAbstractClass

abstract class ALeft {
    abstract fun foo()
}

interface IRight {
    fun foo() {}
}

<!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED!>class CDerived<!> : ALeft(), IRight

abstract class CAbstract : ALeft(), IRight

<!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED!>class CDerivedFromAbstract<!> : CAbstract()

interface ILeft {
    fun foo()
}

abstract class AILeft : ILeft

// Should be ERROR
<!MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED!>class AILeftImpl<!> : AILeft(), IRight

// Should be ERROR
<!MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED!>class RightLeft<!> : ILeft, IRight

interface IBase {
    fun foo()
}

interface IBaseEx : IBase {
    override fun foo() {}
}

abstract class AIBase : IBase

abstract class AIIntermediate : AIBase(), IBaseEx

class Impl : AIIntermediate()