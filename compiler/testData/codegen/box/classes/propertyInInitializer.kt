// IGNORE_BACKEND: JVM_IR
class Outer() {
  val s = "xyzzy"

  open inner class InnerBase(public val name: String) {
  }

  inner class InnerDerived(): InnerBase(s) {
  }

  val x = InnerDerived()
}

fun box() : String {
  val o = Outer()
  return if (o.x.name != "xyzzy") "fail" else "OK"
}
