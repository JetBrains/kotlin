// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

open class A {
    operator fun invoke() {}
    operator fun invoke(f: () -> Unit) {}
}

class B : A() {
    fun bar() {
        <!SUPER_IS_NOT_AN_EXPRESSION!>super<!>()
        (<!SUPER_IS_NOT_AN_EXPRESSION!>super<!>)()
        <!SUPER_IS_NOT_AN_EXPRESSION!>super<!> {}
        (<!SUPER_IS_NOT_AN_EXPRESSION!>super<!>) {}
    }
}