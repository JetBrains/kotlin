// IGNORE_BACKEND_FIR: JVM_IR
data class A(val x: Int) {
  override fun hashCode(): Int = -3
}

fun box(): String {
  return if (A(0).hashCode() == -3) "OK" else "fail"
}