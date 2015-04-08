class Bar {
    init {
        Foo()()
    }
}

class Foo() {
    fun Bar.invoke() {}
}

fun foobar(f: Foo) {
    <selection>Bar().f()</selection>
    Bar().f()
}