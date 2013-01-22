class Wrapper<T>() {
}

fun foo() : Boolean {
  val wrapper = Wrapper<Int>()
  return wrapper is Wrapper<String>
}
