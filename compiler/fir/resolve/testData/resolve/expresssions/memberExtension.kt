class Foo {
    fun bar(arg: Bar) {
        arg.foo()
    }
}

fun Bar.foo() {}

class Bar {
    fun Foo.foo() {}

    fun bar(arg: Foo) {
        arg.foo()
    }
}