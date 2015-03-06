package f

import f.A.Default.B

class A {
    default object {
        class B
    }
}

fun test() = B()