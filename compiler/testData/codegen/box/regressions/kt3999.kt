fun test() {
  val a: (@Extension Function2<*, *, *>).(@Extension Function2<*, *, *>)->Unit = {}
  val b: (@Extension Function2<*, *, *>).(@Extension Function2<*, *, *>)->Unit = {"$this $it"}
  val c: (@Extension Function2<*, *, *>).(@Extension Function2<*, *, *>)->Unit = {}
  a.b(c)
  a b c
}

fun Int.foo(a: Int) = this * a
val boo = fun Int.(a: Int): Int = this + a

fun box(): String {
    test()
    1 foo 2
    3 boo 4
    return "OK"
}
