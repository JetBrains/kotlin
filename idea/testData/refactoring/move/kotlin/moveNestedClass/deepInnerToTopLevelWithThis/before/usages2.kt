package test2

import test.A
import test.A.B.C

fun foo2(): C {
    return A().B().C()
}