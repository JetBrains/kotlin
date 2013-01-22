class Wrapper<T>() {
  fun isSameWrapper(wrapper: Any) = wrapper is Wrapper<T>
}

fun foo() : Boolean {
  val wrapper = Wrapper<Int>()
  return wrapper.isSameWrapper(Wrapper<String>())
}
