package b

import a.A

object B {
    fun baz(a: A) {
        a.foo()
    }
}