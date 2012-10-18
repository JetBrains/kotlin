class MyClass { }

deprecated("'fun rangeTo(i : MyClass)' is deprecated") fun MyClass.rangeTo(i: MyClass): IntIterator {
    throw Exception()
}

fun test() {
    val x1 = MyClass()
    val x2 = MyClass()

    for (i in x1<info descr="'fun rangeTo(i : MyClass)' is deprecated">..</info>x2) {

    }
}

