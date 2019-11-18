// IGNORE_BACKEND_FIR: JVM_IR
class Outer() {
  open inner class InnerBase() {
  }

  inner class InnerDerived(): InnerBase() {
  }

  public val foo: InnerBase? = InnerDerived()
}

fun box() : String {
  val o = Outer()
  return if (o.foo === null) "fail" else "OK"
}
