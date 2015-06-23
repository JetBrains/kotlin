package test

interface Interface {
    fun foo() {
    }
}

open class Klass : Interface {
    override fun foo() {
    }

    open fun bar(a: Int) {
    }

    fun /*rename*/bar(a: String) {
    }
}

class Subclass: Klass() {
    override fun bar(a: Int) {
    }
}