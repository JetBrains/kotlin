open class Protected {
    protected fun bar() {}

    fun baz() {
        bar()
        Nested().foo()
    }

    inner class Inner {
        fun foo() {
            bar()
        }
    }

    protected open class Nested {
        fun foo() {
            bar()
        }

        protected fun bar() {}
    }

    protected companion object {
        fun fromCompanion() {}

        protected fun protectedFromCompanion() {}
    }
}

class Derived : Protected() {
    fun foo() {
        bar()
        Nested().foo()
        Nested().<!HIDDEN!>bar<!>() // hidden

        fromCompanion()
        protectedFromCompanion()
    }

    private class NestedDerived : Nested() {
        fun use() {
            bar()
        }
    }
}

fun test() {
    Protected().baz()
    Protected().Inner()

    Protected().<!HIDDEN!>bar<!>() // hidden
    Protected.<!HIDDEN!>Nested<!>() // hidden
}

open class Generic<T>(val x: T) {
    protected open fun foo(): T = x
}

class DerivedGeneric : Generic<Int>(1) {
    override fun foo(): Int {
        return super.foo()
    }
}
