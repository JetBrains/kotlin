// FIR_IDENTICAL
// !DIAGNOSTICS: -NOTHING_TO_INLINE

open class A {
    protected val value = 2
    protected fun foo(): Int {
        return 1
    }
}

inline fun bar1() = object : A() {
    val x = foo()
    val y: Int
    init {
        y = value
        foo()
    }

    fun baz() {
        value
        foo()
    }
}

inline fun bar2() {
    val obj = object : A() {
        val x = foo()
        val y = value
        init {
            foo()
        }

        fun baz() {
            value
            foo()
        }
    }

    val num = object : A() {
        val x = foo()
        val y = value
        init {
            foo()
        }

        fun baz(): Int {
            return foo() + value
        }
    }.baz()
}
