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
}

// MODULE: main(lib)
// FILE: main.kt
fun test() {
    println(C().<!INVISIBLE_MEMBER!>propA<!>.x())
    println(C().<!INVISIBLE_MEMBER!>propI<!>.x())
    println(C().<!INVISIBLE_MEMBER!>propAI<!>.x())
    println(C().<!INVISIBLE_MEMBER!>propG<!>.x())
}