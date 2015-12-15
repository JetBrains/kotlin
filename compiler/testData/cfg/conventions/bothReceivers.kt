class Bar {
}

class Foo() {
    fun Bar.invoke() {}
}

fun Foo.foobar(bar: Bar) {
    bar()
}