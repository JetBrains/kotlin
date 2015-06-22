package test

interface Interface {
    fun foo() {
    }
}

open class Klass : Interface {
    override fun foo() {
    }

    open fun foo(a: Int) {
    }

    fun /*rename*/foo(a: String) {
    }
}

class Subclass: Klass() {
    override fun foo(a: Int) {
    }
}