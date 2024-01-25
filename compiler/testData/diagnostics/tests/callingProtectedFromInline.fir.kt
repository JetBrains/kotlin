// ISSUE: KT-65058

open class A {
    protected fun foo() {
    }
}

<!NOTHING_TO_INLINE!>inline<!> fun bar() = object : A() {
    fun baz() {
        <!PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>foo<!>()
    }
}
