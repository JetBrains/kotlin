// IGNORE_BACKEND_FIR: JVM_IR
operator fun Int.component1() = this + 1
operator fun Int.component2() = this + 2

fun doTest(): String {
    var s = ""
    for ((a, b) in 0.rangeTo(2)) {
      s += "$a:$b;"
    }
    return s
}

fun box(): String {
  val s = doTest()
  return if (s == "1:2;2:3;3:4;") "OK" else "fail: $s"
}