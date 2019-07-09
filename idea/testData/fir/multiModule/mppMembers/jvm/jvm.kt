actual open class A {
    actual fun foo() {}

    fun bar() {}
}

class C : B() {
    fun test() {
        foo()
        bar()
    }
}

class D : A() {
    fun test() {
        foo()
        bar()
    }
}