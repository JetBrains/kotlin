interface A {
    fun foo() {}
}
interface B : A, E {}
interface C : <error descr="[OTHER_ERROR] Unknown (other) error">B</error> {}
interface D : <error descr="[OTHER_ERROR] Unknown (other) error">B</error> {}
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
    c?.<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: foo">foo</error>()
    d?.<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: foo">foo</error>()
    e?.<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: foo">foo</error>()
    f?.<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: foo">foo</error>()
    g?.<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: foo">foo</error>()
    h?.<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: foo">foo</error>()
}
