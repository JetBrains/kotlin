class MyClass {}

deprecated("Use A instead") fun MyClass.inc(): MyClass { return MyClass() }

fun test() {
    var x3 = MyClass()
    x3<info descr="'fun inc()' is deprecated. Use A instead">++</info>
}