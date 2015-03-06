fun test() {
    <warning descr="'fun test1()' is deprecated. Use A instead">test1</warning>()
    MyClass().<warning descr="'fun test2()' is deprecated. Use A instead">test2</warning>()
    MyClass.<warning descr="'fun test3()' is deprecated. Use A instead">test3</warning>()

    <warning descr="'fun test4(x: kotlin.Int, y: kotlin.Int)' is deprecated. Use A instead">test4</warning>(1, 2)
}

deprecated("Use A instead") fun test1() { }
deprecated("Use A instead") fun test4(x: Int, y: Int) { x + y }

class MyClass() {
    deprecated("Use A instead") fun test2() {}

    default object {
        deprecated("Use A instead") fun test3() {}
    }
}

// NO_CHECK_INFOS
// NO_CHECK_WEAK_WARNINGS