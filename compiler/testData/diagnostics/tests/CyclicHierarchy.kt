trait A {
    fun foo() {}
}
trait B : A, <!CYCLIC_INHERITANCE_HIERARCHY!>E<!> {}
trait C : <!CYCLIC_INHERITANCE_HIERARCHY!>B<!> {}
trait D : <!CYCLIC_INHERITANCE_HIERARCHY!>B<!> {}
trait E : <!CYCLIC_INHERITANCE_HIERARCHY!>F<!> {}
trait F : <!CYCLIC_INHERITANCE_HIERARCHY!>D<!>, <!CYCLIC_INHERITANCE_HIERARCHY!>C<!> {}
trait G : F {}
trait H : F {}

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