// IGNORE_BACKEND_FIR: JVM_IR
fun box(): String {

  fun local():Int {
    return 10;
  }

  class A {
      val test = local()
  }

  return if (A().test == 10) "OK" else "fail"
}