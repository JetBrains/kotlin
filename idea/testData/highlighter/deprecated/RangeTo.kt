class MyClass {
  val i = 1
}

deprecated("Use A instead") fun MyClass.rangeTo(i: MyClass): Iterable<Int> {
    i.i
    throw Exception()
}

fun test() {
    val x1 = MyClass()
    val x2 = MyClass()

    for (i in x1<warning descr="[DEPRECATED_SYMBOL_WITH_MESSAGE] 'rangeTo(MyClass): Iterable<Int>' is deprecated. Use A instead">..</warning>x2) {

    }
}

// NO_CHECK_INFOS
// NO_CHECK_WEAK_WARNINGS

