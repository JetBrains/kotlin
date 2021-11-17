sealed class Foo {
    object A : Foo()
    class B(val i: Int) : Foo()
}

fun test(e: Foo) {
    <caret>when (e) {
    }
}
