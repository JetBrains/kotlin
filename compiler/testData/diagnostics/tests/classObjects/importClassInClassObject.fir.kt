package f

import f.A.Companion.B

class A {
    companion object {
        class B
    }
}

fun test() = B()