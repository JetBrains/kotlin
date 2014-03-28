trait A {
    fun f(): String
}

open class B {
    open fun f(): CharSequence = "charSequence"
}

class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>C<!> : B(), A

val d: A = <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>object<!> : B(), A {}
