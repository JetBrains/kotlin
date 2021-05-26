interface A {
    fun foo() {}
}
interface B : A, <error descr="[CYCLIC_INHERITANCE_HIERARCHY] There's a cycle in the inheritance hierarchy for this type">E</error> {}
interface C : <error descr="[CYCLIC_INHERITANCE_HIERARCHY] There's a cycle in the inheritance hierarchy for this type">B</error> {}
interface D : <error descr="[CYCLIC_INHERITANCE_HIERARCHY] There's a cycle in the inheritance hierarchy for this type">B</error> {}
interface E : <error descr="[CYCLIC_INHERITANCE_HIERARCHY] There's a cycle in the inheritance hierarchy for this type">F</error> {}
interface F : <error descr="[CYCLIC_INHERITANCE_HIERARCHY] There's a cycle in the inheritance hierarchy for this type">D</error>, <error descr="[CYCLIC_INHERITANCE_HIERARCHY] There's a cycle in the inheritance hierarchy for this type">C</error> {}
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
