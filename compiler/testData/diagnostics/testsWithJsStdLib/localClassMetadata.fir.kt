// MODULE: lib
// FILE: lib.kt

interface I {
    fun foo(): String
}

abstract class A {
    abstract fun bar(): String
}

abstract class G<T> {
    abstract fun baz(): T
}

class C {
    private val propA = object : A() {
        override fun bar() = "propA.bar"

        fun x() = "OK"
    }

    private val propI = object : I {
        override fun foo() = "propI.foo"

        fun x() = "OK"
    }

    private val propAI = object : A(), I {
        override fun foo() = "propAI.foo"

        override fun bar() = "propAI.bar"

        fun x() = "OK"
    }

    private val propG = object : G<String>() {
        override fun baz() = "propG.baz"

        fun x() = "OK"
    }

    private val propOI = object {
        inner class D {
            fun df() {}
        }
        fun d(): D = D()
    }.d()

    private val propL = run {
        class L {
            fun l() = "propL.l"
        }
        L()
    }

    private val propL2 = run {
        class L {
            inner class L1 {
                inner class L2 {
                    fun l2() = "propL2.l2"
                }
            }
        }

        L().L1().L2()
    }
}

// MODULE: main(lib)
// FILE: main.kt
fun test() {
    println(C().<!INVISIBLE_REFERENCE!>propA<!>.x())
    println(C().<!INVISIBLE_REFERENCE!>propI<!>.x())
    println(C().<!INVISIBLE_REFERENCE!>propAI<!>.x())
    println(C().<!INVISIBLE_REFERENCE!>propG<!>.x())
    println(C().<!INVISIBLE_REFERENCE!>propOI<!>.df())
    println(C().<!INVISIBLE_REFERENCE!>propL<!>.l())
    println(C().<!INVISIBLE_REFERENCE!>propL2<!>.l2())
}
