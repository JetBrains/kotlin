abstract class A {
    fun foo() {}
}

interface Y {
    fun baz() {}
}

open class B : A(), Y {
    fun bar() {
        foo()
        baz()
    }
}

class C : B() {
    fun test() {
        foo()
        bar()
        baz()
    }
}