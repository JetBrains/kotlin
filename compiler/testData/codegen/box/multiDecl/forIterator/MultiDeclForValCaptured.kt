// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
class C(val i: Int) {
  operator fun component1() = i + 1
  operator fun component2() = i + 2
}

fun doTest(l : ArrayList<C>): String {
    var s = ""
    for ((a, b) in l) {
      s += {"$a:$b;"}()
    }
    return s
}

fun box(): String {
  val l = ArrayList<C>()
  l.add(C(0))
  l.add(C(1))
  l.add(C(2))
  val s = doTest(l)
  return if (s == "1:2;2:3;3:4;") "OK" else "fail: $s"
}