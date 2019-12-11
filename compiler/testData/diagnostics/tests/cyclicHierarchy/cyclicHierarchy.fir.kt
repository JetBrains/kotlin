interface A {
    fun foo() {}
}
interface B : A, E {}
interface C : <!OTHER_ERROR!>B<!> {}
interface D : <!OTHER_ERROR!>B<!> {}
interface E : F {}
interface F : D, C {}
interface G : F {}
interface H : F {}

val a : A? = null
val b : B? = null
val c : C? = null
val d : D? = null
val e : E? = null
val f : F? = null
val g : G? = null
val h : H? = null

fun test() {
    a?.foo()
    b?.foo()
    c?.<!UNRESOLVED_REFERENCE!>foo<!>()
    d?.<!UNRESOLVED_REFERENCE!>foo<!>()
    e?.<!UNRESOLVED_REFERENCE!>foo<!>()
    f?.<!UNRESOLVED_REFERENCE!>foo<!>()
    g?.<!UNRESOLVED_REFERENCE!>foo<!>()
    h?.<!UNRESOLVED_REFERENCE!>foo<!>()
}