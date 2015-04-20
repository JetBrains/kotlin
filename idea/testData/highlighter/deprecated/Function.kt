fun test() {
    <warning descr="[DEPRECATED_SYMBOL_WITH_MESSAGE] 'test1(): Unit' is deprecated. Use A instead">test1</warning>()
    MyClass().<warning descr="[DEPRECATED_SYMBOL_WITH_MESSAGE] 'test2(): Unit' is deprecated. Use A instead">test2</warning>()
    MyClass.<warning descr="[DEPRECATED_SYMBOL_WITH_MESSAGE] 'test3(): Unit' is deprecated. Use A instead">test3</warning>()

    <warning descr="[DEPRECATED_SYMBOL_WITH_MESSAGE] 'test4(Int, Int): Unit' is deprecated. Use A instead">test4</warning>(1, 2)
}

deprecated("Use A instead") fun test1() { }
deprecated("Use A instead") fun test4(x: Int, y: Int) { x + y }

class MyClass() {
    deprecated("Use A instead") fun test2() {}

    companion object {
        deprecated("Use A instead") fun test3() {}
    }
}

// NO_CHECK_INFOS
// NO_CHECK_WEAK_WARNINGS