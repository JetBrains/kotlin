// PROBLEM: none
fun test(foo: Foo) {
    <caret>if (foo is Foo) {
        foo.bar()
    }
}

class Foo {
    fun bar() {}
}
