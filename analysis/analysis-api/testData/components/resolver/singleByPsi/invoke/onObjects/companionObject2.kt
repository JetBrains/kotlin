package foo

class Foo(i: Int) {
    companion object {
        operator fun invoke() {}
    }
}

fun test() {
    foo.Fo<caret>o()
}
