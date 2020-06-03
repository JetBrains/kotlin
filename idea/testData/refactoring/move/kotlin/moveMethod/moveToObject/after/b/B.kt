package b

import a.A

object B {
    fun baz(a: A) {
        B.foo(a)
    }

    fun foo(a: A) {
        a.bar()
    }
}