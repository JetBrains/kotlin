// WITH_RUNTIME
// PROBLEM: none

class Foo {
    private val foo = object {
        var bar<caret>: Int = 42
    }

    fun baz(value: Int) {
        foo.bar = value
    }
}