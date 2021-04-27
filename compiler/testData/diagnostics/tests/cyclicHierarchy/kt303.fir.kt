// !WITH_NEW_INFERENCE
// KT-303 Stack overflow on a cyclic class hierarchy

open class Foo() : Bar() {
  val a : Int = 1
}

open class Bar() : <!OTHER_ERROR!>Foo<!>() {

}

val x : Int = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>Foo()<!>
