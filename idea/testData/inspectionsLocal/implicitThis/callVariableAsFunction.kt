class Foo {
    operator fun invoke() {}
}

class Bar {
    val foo = Foo()
}

fun Bar.test() {
    <caret>foo()
}
