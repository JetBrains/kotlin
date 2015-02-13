package test

import dependency.A
import dependency.T

fun f(a: A, t: T) {
    a.f()
    a.p
    t.f
    t.p
}