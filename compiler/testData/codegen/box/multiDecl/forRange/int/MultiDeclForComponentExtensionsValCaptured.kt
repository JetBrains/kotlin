// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS_IR
operator fun Int.component1() = this + 1
operator fun Int.component2() = this + 2

fun doTest(): String {
    var s = ""
    for ((a, b) in 0..2) {
      s += {"$a:$b;"}()
    }
    return s
}

fun box(): String {
  val s = doTest()
  return if (s == "1:2;2:3;3:4;") "OK" else "fail: $s"
}