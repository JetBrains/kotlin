package syntheticMethods

fun main(args: Array<String>) {
    val d: Base<String> = Derived()
    //Breakpoint!
    d.foo("")
    A().test()
    A.test()
}

open class Base<T> {
    open fun foo(t: T) {
        val a = 1
    }
}

class Derived: Base<String>() {
    override fun foo(t: String) {
        val a = 1
    }
}

class A {
    fun test() {
        lambda {
            val a = 1
        }
    }

    fun lambda(f: () -> Unit) {
        f()
    }

    default object {
        fun test() {
            lambda {
                val a = 1
            }
        }

        fun lambda(f: () -> Unit) {
            f()
        }
    }
}

// STEP_INTO: 26
// SKIP_SYNTHETIC_METHODS: false