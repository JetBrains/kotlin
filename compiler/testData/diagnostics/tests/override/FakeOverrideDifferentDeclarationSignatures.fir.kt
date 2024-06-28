interface A {
    fun f(): String = "string"
}

open class B {
    open fun f(): CharSequence = "charSequence"
}

<!CANNOT_INFER_VISIBILITY, MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>class C<!> : B(), A

val obj: A = <!CANNOT_INFER_VISIBILITY, MANY_IMPL_MEMBER_NOT_IMPLEMENTED!>object<!> : B(), A {}
