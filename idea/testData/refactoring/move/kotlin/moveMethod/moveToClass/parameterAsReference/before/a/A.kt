package a

import b.B

class A {
    fun foo(b: B) {
        val a1 = b.a
    }

    fun bar(b: B) {
        foo(b)
        val a = A().apply {
            foo(b)
            this@A.foo(b)
        }
    }
}