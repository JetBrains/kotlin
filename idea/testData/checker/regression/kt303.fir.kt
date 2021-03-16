// KT-303 Stack overflow on a cyclic class hierarchy

open class Foo() : Bar() {
  val a : Int = 1
}

open class Bar() : <error descr="[OTHER_ERROR] Unknown (other) error">Foo</error>() {

}

val x : Int = Foo()
