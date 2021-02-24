package a

import b.B

class A(val b: B) {
    fun foo() {
        val a1 = b.a
        bar()
    }

    fun bar() {}
}