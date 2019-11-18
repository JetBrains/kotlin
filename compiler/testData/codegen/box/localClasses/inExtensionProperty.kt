// IGNORE_BACKEND_FIR: JVM_IR
package test

val A.a: String
  get() {
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
    return A().a
}