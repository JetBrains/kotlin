// IGNORE_BACKEND_FIR: JVM_IR
fun foo(a: Int, vararg b: Int, f: (IntArray) -> String): String {
  return "test" + a + " " + f(b)
}

fun box(): String {
  val test1 = foo(1) {a -> "" + a.size}
  if (test1 != "test1 0") return test1

  val test2 = foo(2, 2) {a -> "" + a.size}
  if (test2 != "test2 1") return test2

  val test3 = foo(3, 2, 3) {a -> "" + a.size}
  if (test3 != "test3 2") return test3

  return "OK"
}
