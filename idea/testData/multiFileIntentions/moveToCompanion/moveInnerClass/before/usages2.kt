package test2

import test.A
import test.A.B

fun foo2(): B {
    return A().B()
}