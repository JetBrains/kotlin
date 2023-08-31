fun test(foo: Any?) {
    if (foo is Foo) {
        <caret>val x = 0
    }
}

class Foo {
    fun call() {}
}