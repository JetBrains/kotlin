class MyClass {
  val i = 0
}

deprecated("Use A instead") fun MyClass.inc(): MyClass { return MyClass() }

fun test() {
    var x3 = MyClass()
    x3<warning descr="'fun inc()' is deprecated. Use A instead">++</warning>
    x3.i
}