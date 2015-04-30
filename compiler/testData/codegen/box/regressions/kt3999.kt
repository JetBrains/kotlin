class A() {
  fun A.invoke(a: A) = "$this ${this@A} $a"
}

fun test1() {
  val a = A()
  val b = A()
  val c = A()
  a.b(c)
  a b c
}

fun test2() {
  val a: (@extension Function2<*, *, *>).(@extension Function2<*, *, *>)->Unit = {}
  val b: (@extension Function2<*, *, *>).(@extension Function2<*, *, *>)->Unit = {"$this $it"}
  val c: (@extension Function2<*, *, *>).(@extension Function2<*, *, *>)->Unit = {}
  a.b(c)
  a b c
}

fun Int.foo(a: Int) = this * a
val boo = fun Int.(a: Int): Int = this + a

fun box(): String {
    test1()
    test2()
    1 foo 2
    3 boo 4
    return "OK"
}
