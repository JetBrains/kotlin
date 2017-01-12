// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME

data class A(val x: Int) {
  fun toString(other: Any): String = ""
}

data class B(val x: Int) {
  fun toString(other: B, another: Any): String = ""
}

fun box(): String {
  A::class.java.getDeclaredMethod("toString")
  A::class.java.getDeclaredMethod("toString", Any::class.java)

  B::class.java.getDeclaredMethod("toString")
  B::class.java.getDeclaredMethod("toString", B::class.java, Any::class.java)

  return "OK"
}
