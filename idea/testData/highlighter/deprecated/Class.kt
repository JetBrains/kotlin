package test

<info>import</info> java.util.ArrayList

Deprecated <info>open</info> class MyClass {}

fun test() {
    val a : <info descr="'test.MyClass' is deprecated">MyClass</info>? = null
    val b = <info descr="'test.MyClass' is deprecated">MyClass</info>()
    val c = ArrayList<<info descr="'test.MyClass' is deprecated">MyClass</info>>()
}

class Test(): <info descr="'test.MyClass' is deprecated">MyClass</info>() {}

class Test2(param: <info descr="'test.MyClass' is deprecated">MyClass</info>) {}