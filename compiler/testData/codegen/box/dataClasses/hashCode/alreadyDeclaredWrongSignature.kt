// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_RUNTIME

data class A(val x: Int) {
  fun hashCode(other: Any): Int = 0
}

data class B(val x: Int) {
  fun hashCode(other: B, another: Any): Int = 0
}

fun box(): String {
  A::class.java.getDeclaredMethod("hashCode")
  A::class.java.getDeclaredMethod("hashCode", Any::class.java)

  B::class.java.getDeclaredMethod("hashCode")
  B::class.java.getDeclaredMethod("hashCode", B::class.java, Any::class.java)

  return "OK"
}
