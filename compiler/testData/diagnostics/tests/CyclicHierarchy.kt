trait A {
    fun foo() {}
}
trait B : A, <!CYCLIC_INHERITANCE_HIERARCHY!>E<!> {}
trait C : B {}
trait D : <!CYCLIC_INHERITANCE_HIERARCHY!>B<!> {}
trait E : <!CYCLIC_INHERITANCE_HIERARCHY!>F<!> {}
trait F : <!CYCLIC_INHERITANCE_HIERARCHY!>D<!>, C {}
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
    c?.foo()
    d?.foo()
    e?.<!UNRESOLVED_REFERENCE!>foo<!>()
    f?.foo()
    g?.foo()
    h?.foo()
}
