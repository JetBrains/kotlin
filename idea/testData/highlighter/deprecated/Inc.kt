class MyClass {
  val i = 0
}

deprecated("Use A instead") fun MyClass.inc(): MyClass { return MyClass() }

fun test() {
    var x3 = MyClass()
    x3<warning descr="[DEPRECATED_SYMBOL_WITH_MESSAGE] 'inc(): MyClass' is deprecated. Use A instead">++</warning>
    x3.i
}

// NO_CHECK_INFOS
// NO_CHECK_WEAK_WARNINGS