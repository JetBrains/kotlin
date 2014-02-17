package to

import d.A
import d.T
import d.g
import d.c
import d.ext
import d.O1
import d.O2
import d.E.ENTRY
import d.Outer.Inner
import d.Outer.Nested
import d.Outer.NestedEnum
import d.Outer.NestedObj
import d.Outer.NestedTrait
import d.Outer.NestedAnnotation
import d.ClassObject

fun f(a: A, t: T) {
    g(A(c).ext())
    O1.f()
    O2
    ENTRY
}

fun f2(i: Inner, n: Nested, e: NestedEnum, o: NestedObj, t: NestedTrait, a: NestedAnnotation) {
    ClassObject
}