open class A {
    companion object {
        fun foo() {}
    }

    fun bar() {
        foo()
    }
}

class B {
    companion object : A() {
        fun baz() {}
    }
}

fun test() {
    A.foo()
    B.bar()
    B.baz()
}