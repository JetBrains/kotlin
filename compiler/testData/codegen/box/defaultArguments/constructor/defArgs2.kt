// IGNORE_BACKEND_FIR: JVM_IR
class A(val a: Int = 0, val b: String = "a")

fun box(): String {
  val a1 = A()
  val a2 = A(1)
  val a3 = A(b = "b")
  val a4 = A(2, "c")
  if (a1.a != 0 && a1.b != "a") return "fail"
  if (a2.a != 1 && a2.b != "a") return "fail"
  if (a3.a != 0 && a3.b != "b") return "fail"
  if (a4.a != 2 && a4.b != "c") return "fail"
  return "OK"
}
