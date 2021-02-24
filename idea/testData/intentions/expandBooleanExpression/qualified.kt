fun test(b: Boolean) {
    val b = Foo().bar()<caret>
}

class Foo {
    fun bar() = true
}