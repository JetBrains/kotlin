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
            <!DEBUG_INFO_MISSING_UNRESOLVED!>super<!><!UNRESOLVED_REFERENCE!>@Context<!>.<!DEBUG_INFO_MISSING_UNRESOLVED!>h<!>()
        }
    }
}