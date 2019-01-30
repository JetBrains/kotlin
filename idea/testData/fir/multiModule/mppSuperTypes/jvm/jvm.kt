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
        // This and next cannot be resolved yet due to lack of search symbols / projections
        bar()
        baz()
    }
}

class D : A() {
    fun test() {
        foo()
        bar()
        baz()
    }
}