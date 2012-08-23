class C(val i: Int) {
}

fun C.component1() = i + 1
fun C.component2() = i + 2

fun doTest(l : java.util.ArrayList<C>): String {
    var s = ""
    for ((a, b) in l) {
      s += "$a:$b;"
    }
    return s
}

fun box(): String {
  val l = java.util.ArrayList<C>()
  l.add(C(0))
  l.add(C(1))
  l.add(C(2))
  val s = doTest(l)
  return if (s == "1:2;2:3;3:4;") "OK" else "fail: $s"
}