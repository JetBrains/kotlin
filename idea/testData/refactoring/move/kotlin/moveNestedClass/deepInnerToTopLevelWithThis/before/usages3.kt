package test2

import test.A
import test.A.B

fun foo2(): B.C {
    return A().B().C()
}