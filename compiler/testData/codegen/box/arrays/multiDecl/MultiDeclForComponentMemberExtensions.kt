// IGNORE_BACKEND_FIR: JVM_IR
class C(val i: Int) {
}

class M {
  operator fun C.component1() = i + 1
  operator fun C.component2() = i + 2

  fun doTest(l : Array<C>): String {
      var s = ""
      for ((a, b) in l) {
        s += "$a:$b;"
      }
      return s
  }
}

fun box(): String {
  val l = Array<C>(3, {x -> C(x)})
  val s = M().doTest(l)
  return if (s == "1:2;2:3;3:4;") "OK" else "fail: $s"
}