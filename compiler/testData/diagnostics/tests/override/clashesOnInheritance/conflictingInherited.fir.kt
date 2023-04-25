// SKIP_TXT

open class A {
    fun <T> some(s: String): T = null!!
}

interface I {
    fun some(text: String) = ""
}

open <!CONFLICTING_INHERITED_MEMBERS!>class X1<!> : A(), I
open <!CONFLICTING_INHERITED_MEMBERS!>class X2<!> : X1()

// for some reason no error in K1
open class B {
    fun <T> some(s: String): T = null!!

    fun some(text: String) = ""
}

open class X3 : B()
open class X4 : X3()

open class C {
    fun <T> some(s: String): T = null!!
}

open class X5 : C() {
    <!CONFLICTING_OVERLOADS!>fun some(text: String)<!> = ""
}
open <!CONFLICTING_INHERITED_MEMBERS!>class X6<!> : X5()
