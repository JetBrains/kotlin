trait A {
    fun f(): String = "string"
}

open class B {
    open fun f(): CharSequence = "charSequence"
}

class <!MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>C<!> : B(), A

val obj: A = <!MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>object<!> : B(), A {}
