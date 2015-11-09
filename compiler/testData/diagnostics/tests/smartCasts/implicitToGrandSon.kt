open class A {
    open fun foo() = "FAIL"

    fun bar() = if (this is C) <!DEBUG_INFO_IMPLICIT_RECEIVER_SMARTCAST!>foo<!>() else "FAIL"
}

open class B : A()

open class C : B() {
    override fun foo() = "OK"
}
