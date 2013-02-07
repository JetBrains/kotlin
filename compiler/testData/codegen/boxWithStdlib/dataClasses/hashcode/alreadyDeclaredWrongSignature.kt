data class A(val x: Int) {
  fun hashCode(other: Any): Int = 0
}

data class B(val x: Int) {
  fun hashCode(other: B, another: Any): Int = 0
}

fun box(): String {
  javaClass<A>().getDeclaredMethod("hashCode")
  javaClass<A>().getDeclaredMethod("hashCode", javaClass<Any>())

  javaClass<B>().getDeclaredMethod("hashCode")
  javaClass<B>().getDeclaredMethod("hashCode", javaClass<B>(), javaClass<Any>())

  return "OK"
}