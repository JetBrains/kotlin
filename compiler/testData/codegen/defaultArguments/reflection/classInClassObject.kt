class A {
    default object {
        class Foo(val a: Int = 1) {}
    }
}

// CLASS: A$Default$Foo
// HAS_DEFAULT_CONSTRUCTOR: true
