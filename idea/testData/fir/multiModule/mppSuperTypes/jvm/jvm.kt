abstract class X {
    fun bar() {}
}

interface Y {
    fun baz() {}
}

actual open class A : X(), Y {
    actual fun foo() {}
}

class C : B() {
    fun test() {
        foo()
        // This cannot be resolved yet due to lack of search symbols / projections
        bar()
        // This cannot be resolved yet due to lack of interface lookup
        baz()
    }
}

class D : A() {
    fun test() {
        foo()
        bar()
        // This cannot be resolved yet due to lack of interface lookup
        baz()
    }
}