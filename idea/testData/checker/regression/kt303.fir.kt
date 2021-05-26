// KT-303 Stack overflow on a cyclic class hierarchy

open class Foo() : <error descr="[CYCLIC_INHERITANCE_HIERARCHY] There's a cycle in the inheritance hierarchy for this type">Bar</error>() {
  val a : Int = 1
}

open class Bar() : <error descr="[CYCLIC_INHERITANCE_HIERARCHY] There's a cycle in the inheritance hierarchy for this type">Foo</error>() {

}

val x : Int = <error descr="[INITIALIZER_TYPE_MISMATCH] Initializer type mismatch: expected kotlin/Int, actual Foo"><error descr="[TYPE_MISMATCH] Type mismatch: inferred type is Foo but kotlin/Int was expected">Foo()</error></error>
