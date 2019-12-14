// "Replace with 'Bar::class.java'" "true"
// WITH_RUNTIME
class Foo {
    companion object Bar
}

fun test() {
    Foo.javaClass<caret>
}