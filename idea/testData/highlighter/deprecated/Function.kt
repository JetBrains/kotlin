fun test() {
    <warning descr="[DEPRECATION] 'test1(): Unit' is deprecated. Use A instead">test1</warning>()
    MyClass().<warning descr="[DEPRECATION] 'test2(): Unit' is deprecated. Use A instead">test2</warning>()
    MyClass.<warning descr="[DEPRECATION] 'test3(): Unit' is deprecated. Use A instead">test3</warning>()

    <warning descr="[DEPRECATION] 'test4(Int, Int): Unit' is deprecated. Use A instead">test4</warning>(1, 2)
}

@Deprecated("Use A instead") fun test1() { }
@Deprecated("Use A instead") fun test4(x: Int, y: Int) { x + y }

class MyClass() {
    @Deprecated("Use A instead") fun test2() {}

    companion object {
        @Deprecated("Use A instead") fun test3() {}
    }
}

// NO_CHECK_INFOS
// NO_CHECK_WEAK_WARNINGS
