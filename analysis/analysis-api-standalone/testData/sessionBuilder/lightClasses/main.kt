package org.test

class KotlinClass {
    fun foo(n: Int): String {
        return n.toString()
    }
}

fun foo() {
    KotlinClass().foo()
}