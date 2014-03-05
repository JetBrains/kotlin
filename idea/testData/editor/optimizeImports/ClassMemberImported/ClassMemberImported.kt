package test

import dependency.*
import dependency.A.f
import dependency.A.p
import dependency.T.f
import dependency.T.p

fun f(a: A, t: T) {
    a.f()
    a.p
    t.f
    t.p
}