// IGNORE_BACKEND_FIR: JVM_IR
operator fun Long.component1() = this + 1
operator fun Long.component2() = this + 2

fun doTest(l : Array<Long>): String {
    var s = ""
    for ((a, b) in l) {
      s += {"$a:$b;"}()
    }
    return s
}

fun box(): String {
  val l = Array<Long>(3, {x -> x.toLong()})
  val s = doTest(l)
  return if (s == "1:2;2:3;3:4;") "OK" else "fail: $s"
}