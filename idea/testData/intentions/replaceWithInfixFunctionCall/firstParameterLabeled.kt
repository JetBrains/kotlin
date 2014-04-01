fun doSomething<T>(a: T) {}

class Foo {
    fun foo(x: Int) {
        doSomething("lol")
    }
}

fun bar(baz: Foo) {
    baz.<caret>foo(x = 1)
}
