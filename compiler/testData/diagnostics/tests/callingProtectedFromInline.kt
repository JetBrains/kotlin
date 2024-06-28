// ISSUE: KT-65058

open class A {
    protected fun foo() {
    }
}

<!NOTHING_TO_INLINE!>inline<!> fun bar() = object : A() {
    fun baz() {
        foo()
        this.foo()

        val receiver: A = this
        receiver.<!INVISIBLE_MEMBER, PROTECTED_CALL_FROM_PUBLIC_INLINE_ERROR!>foo<!>()

        val receiver2 = this
        receiver2.foo()
    }
}
