package b

import a.A

class B (val a: A) {
    fun baz() {
        a.b.foo(a)
    }

    fun foo(a: A) {
        val a1 = this.a
        a.bar()
    }
}