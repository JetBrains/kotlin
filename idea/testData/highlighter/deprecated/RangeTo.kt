class MyClass {
  val i = 1
}

deprecated("Use A instead") fun MyClass.rangeTo(i: MyClass): IntIterator {
    i.i
    throw Exception()
}

fun test() {
    val x1 = MyClass()
    val x2 = MyClass()

    for (i in x1<warning descr="'fun rangeTo(i: MyClass)' is deprecated. Use A instead">..</warning>x2) {

    }
}

