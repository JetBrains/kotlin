// !LANGUAGE: +ContextReceivers

interface Context {
    fun h() {}
}

open class A {
    open fun f() {}
}

class B : A() {
    override fun f() {}

    context(Context)
    inner class C {
        fun g() {
            super@B.f()
            <!SUPERCLASS_NOT_ACCESSIBLE_FROM_INTERFACE!>super@Context<!>.<!UNRESOLVED_REFERENCE!>h<!>()
        }
    }
}
