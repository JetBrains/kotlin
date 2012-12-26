enum class Foo(val a: Int = 1) {
    A: Foo()
}

// CLASS: Foo
// HAS_DEFAULT_CONSTRUCTOR: false
