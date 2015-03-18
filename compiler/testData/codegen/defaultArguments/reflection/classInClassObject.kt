class A {
    companion object {
        class Foo(val a: Int = 1) {}
    }
}

// CLASS: A$Companion$Foo
// HAS_DEFAULT_CONSTRUCTOR: true
