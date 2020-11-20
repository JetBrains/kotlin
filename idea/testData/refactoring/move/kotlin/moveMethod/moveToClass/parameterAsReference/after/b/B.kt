package b

import a.A

class B (val a: A) {
    fun baz(b: B) {
        b.foo()
    }

    fun foo() {
        val a1 = this.a
    }
}