// PROBLEM: none
fun test(foo: Foo) {
    <caret>if (foo != null) {
        foo.bar()
    }
}

class Foo {
    fun bar() {}
}
