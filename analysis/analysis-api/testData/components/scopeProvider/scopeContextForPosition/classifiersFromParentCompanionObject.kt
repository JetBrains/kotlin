open class Base {
  companion object {
    class Nested

    fun bar(): String = "Hello"
  }
}

class Child : Base() {
  val foo = <expr>e</expr>
}