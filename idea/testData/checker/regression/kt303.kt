// KT-303 Stack overflow on a cyclic class hierarchy

open class Foo() : <error>Bar</error>() {
  val a : Int = 1
}

open class Bar() : <error>Foo</error>() {

}

val x : Int = <error>Foo()</error>