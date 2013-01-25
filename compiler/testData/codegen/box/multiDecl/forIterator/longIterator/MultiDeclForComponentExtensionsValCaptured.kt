fun Long.component1() = this + 1
fun Long.component2() = this + 2

fun doTest(l : java.util.ArrayList<Long>): String {
    var s = ""
    for ((a, b) in l) {
      s += {"$a:$b;"}()
    }
    return s
}

fun box(): String {
  val l = java.util.ArrayList<Long>()
  l.add(0)
  l.add(1)
  l.add(2)
  val s = doTest(l)
  return if (s == "1:2;2:3;3:4;") "OK" else "fail: $s"
}