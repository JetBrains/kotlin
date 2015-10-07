abstract class ALeft {
    abstract fun foo()
}

interface IRight {
    fun foo() {}
}

<!ABSTRACT_CLASS_MEMBER_NOT_IMPLEMENTED!>class CDerived<!> : ALeft(), IRight