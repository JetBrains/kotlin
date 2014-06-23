class Bar {
}

class Foo() {
    fun Bar.invoke() {}
}

fun foobar(f: Foo) {
    Bar().f()
}