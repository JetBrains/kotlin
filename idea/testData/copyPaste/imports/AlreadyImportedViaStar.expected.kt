// ERROR: Type mismatch: inferred type is Unit but A was expected
package to

import a.*

fun f(p: A, t: T) {
    g(A(c).ext())
    O1.f()
    O2
    E.ENTRY
}

fun f2(i: Outer.Inner, n: Outer.Nested, e: Outer.NestedEnum, o: Outer.NestedObj, t: Outer.NestedTrait, a: Outer.NestedAnnotation) {
    ClassObject
}