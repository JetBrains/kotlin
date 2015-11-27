package syntheticMethodsSkip

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
            1
        }
    }

    fun lambda(f: () -> Int): Int {
        return f()
    }

    companion object {
        fun test() {
            lambda {
                1
            }
        }

        fun lambda(f: () -> Int): Int {
            return f()
        }
    }
}

// STEP_INTO: 26
// SKIP_SYNTHETIC_METHODS: true
// SKIP_CONSTRUCTORS: true