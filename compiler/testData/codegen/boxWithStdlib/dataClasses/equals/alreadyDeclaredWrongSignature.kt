data class A(val x: Int) {
  fun equals(other: Any): Boolean = false
}

data class B(val x: Int) {
  fun equals(other: B): Boolean = false
}

data class C(val x: Int) {
  fun equals(): Boolean = false
}

data class D(val x: Int) {
  fun equals(other: Any?, another: String): Boolean = false
}

data class E(val x: Int) {
  fun equals(x: E): Boolean = false
  fun equals(x: Any?): Boolean = false
}

fun box(): String {
  javaClass<A>().getDeclaredMethod("equals", javaClass<Any>())

  javaClass<B>().getDeclaredMethod("equals", javaClass<Any>())
  javaClass<B>().getDeclaredMethod("equals", javaClass<B>())

  javaClass<C>().getDeclaredMethod("equals", javaClass<Any>())
  javaClass<C>().getDeclaredMethod("equals")

  javaClass<D>().getDeclaredMethod("equals", javaClass<Any>())
  javaClass<D>().getDeclaredMethod("equals", javaClass<Any>(), javaClass<String>())

  javaClass<E>().getDeclaredMethod("equals", javaClass<Any>())
  javaClass<E>().getDeclaredMethod("equals", javaClass<E>())

  return "OK"
}