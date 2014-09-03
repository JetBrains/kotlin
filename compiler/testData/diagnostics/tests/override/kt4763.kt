trait A {
    fun f(): String
}

open class B {
    open fun f(): CharSequence = "charSequence"
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class C<!> : B(), A

val d: A = <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>object<!> : B(), A {}
