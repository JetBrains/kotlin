package a

import b.B

class A(val b: B) {
    fun foo() {
        bar()
    }

    fun bar() {}
}