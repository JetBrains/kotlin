class A {
    public class Foo(val a: Int = 1) {}

    fun foo() {
        Foo()
    }
}

// CLASS: A$Foo
// HAS_DEFAULT_CONSTRUCTOR: false
