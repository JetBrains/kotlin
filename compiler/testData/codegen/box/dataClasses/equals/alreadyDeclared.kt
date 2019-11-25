// IGNORE_BACKEND_FIR: JVM_IR
data class A(val x: Int) {
  override fun equals(other: Any?): Boolean = false
}

fun box(): String {
  val a = A(0)
  return if (a.equals(a)) "fail" else "OK"
}