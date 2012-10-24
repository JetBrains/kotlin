fun test() {
    <info descr="'fun test1()' is deprecated. Use A instead">test1</info>()
    MyClass().<info descr="'fun test2()' is deprecated. Use A instead">test2</info>()
    MyClass.<info descr="'fun test3()' is deprecated. Use A instead">test3</info>()

    <info descr="'fun test4(x : jet.Int, y : jet.Int)' is deprecated. Use A instead">test4</info>(1, 2)
}

deprecated("Use A instead") fun test1() { }
deprecated("Use A instead") fun test4(x: Int, y: Int) { }

class MyClass() {
    deprecated("Use A instead") fun test2() {}

    class object {
        deprecated("Use A instead") fun test3() {}
    }
}