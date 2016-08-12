class MyClass {
  val i = 1
}

@Deprecated("Use A instead") operator fun MyClass.rangeTo(i: MyClass): Iterable<Int> {
    i.i
    throw Throwable()
}

fun test() {
    val x1 = MyClass()
    val x2 = MyClass()

    for (i in x1<warning descr="[DEPRECATION] 'rangeTo(MyClass): Iterable<Int>' is deprecated. Use A instead">..</warning>x2) {

    }
}

// NO_CHECK_INFOS
// NO_CHECK_WEAK_WARNINGS

