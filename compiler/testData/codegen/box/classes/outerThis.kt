// IGNORE_BACKEND_FIR: JVM_IR
class Outer() {
  inner class Inner() {
    val outer: Outer get() = this@Outer
  }

  public val x : Inner = Inner()
}

fun box() : String {
  val o = Outer()
  return if (o === o.x.outer) "OK" else "fail"
}
