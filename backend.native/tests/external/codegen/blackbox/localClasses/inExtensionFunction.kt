package test

fun A.a(): String {
  class B {
      val b : String
          get() = this@a.s
  }
  return B().b
}

class A {
    val s : String = "OK"
}

fun box() : String {
    return A().a()
}