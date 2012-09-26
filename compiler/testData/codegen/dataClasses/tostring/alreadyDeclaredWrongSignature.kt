data class A(val x: Int) {
  fun toString(other: Any): String = ""
}

data class B(val x: Int) {
  fun toString(other: B, another: Any): String = ""
}

fun box(): String {
  javaClass<A>().getDeclaredMethod("toString")
  javaClass<A>().getDeclaredMethod("toString", javaClass<Any>())

  javaClass<B>().getDeclaredMethod("toString")
  javaClass<B>().getDeclaredMethod("toString", javaClass<B>(), javaClass<Any>())

  return "OK"
}