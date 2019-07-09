// WITH_RUNTIME
// IS_APPLICABLE: false

class Foo {
    fun foo(i: Int) {}
}

fun bar(i: Int, f: Foo) {}

fun test(f: Foo) {
    f.foo(1)
    bar(2, f)<caret>
}