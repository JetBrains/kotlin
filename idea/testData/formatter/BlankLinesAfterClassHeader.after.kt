class Foo {
    fun bar() {
    }
}

object Bar {
    fun xyzzy() {
    }
}

val f = object {
    fun foo() {}
}

class FooM
    : IFoo {

    fun bar()
}

class FooC(
        val x: String
) {

    fun bar()
}

enum class Foo { Bar, Baz }

class Foo {
    fun bar() {}
}

// SET_INT: BLANK_LINES_AFTER_CLASS_HEADER = 1
