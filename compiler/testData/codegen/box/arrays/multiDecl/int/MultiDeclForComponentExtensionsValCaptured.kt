// IGNORE_BACKEND: JS_IR
operator fun Int.component1() = this + 1
operator fun Int.component2() = this + 2

fun doTest(l : Array<Int>): String {
    var s = ""
    for ((a, b) in l) {
      s += {"$a:$b;"}()
    }
    return s
}

fun box(): String {
  val l = Array<Int>(3, {x -> x})
  val s = doTest(l)
  return if (s == "1:2;2:3;3:4;") "OK" else "fail: $s"
}