class A {
  companion object {
    fun foo() = "OK"
  }
}

fun box() = A.foo()
