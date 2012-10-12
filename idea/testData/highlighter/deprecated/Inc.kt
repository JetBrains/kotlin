class MyClass {}

Deprecated fun MyClass.inc(): MyClass { return MyClass() }

fun test() {
    var x3 = MyClass()
    x3<info descr="'fun inc()' is deprecated">++</info>
}