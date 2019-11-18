// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
operator fun Long.component1() = this + 1
operator fun Long.component2() = this + 2

fun doTest(l : ArrayList<Long>): String {
    var s = ""
    for ((a, b) in l) {
      s += "$a:$b;"
    }
    return s
}

fun box(): String {
  val l = ArrayList<Long>()
  l.add(0)
  l.add(1)
  l.add(2)
  val s = doTest(l)
  return if (s == "1:2;2:3;3:4;") "OK" else "fail: $s"
}