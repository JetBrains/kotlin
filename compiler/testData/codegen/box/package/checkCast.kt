// IGNORE_BACKEND_FIR: JVM_IR
class C(val x: Int) {
  override fun equals(rhs: Any?): Boolean {
    if (rhs is C) {
      val rhsC = rhs as C
      return rhsC.x == x
    }
    return false
  }
}

fun box(): String {
  val c1 = C(10)
  val c2 = C(10)
  return if (c1 == c2) "OK" else "fail"
}
