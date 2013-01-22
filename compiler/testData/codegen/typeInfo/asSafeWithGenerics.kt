class Wrapper<T>() {
  fun castToSelf(wrapper: Any) : Wrapper<T>? = wrapper as? Wrapper<T>
}

fun foo() : Wrapper<Int>? {
  val wrapper = Wrapper<Int>()
  return wrapper.castToSelf(Wrapper<String>())
}
