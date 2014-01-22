package to

import a.Outer.Inner
import a.Outer.Nested
import a.Outer.NestedEnum
import a.Outer.NestedObj
import a.Outer.NestedTrait
import a.Outer.NestedAnnotation
import a.Outer

fun f(i: Inner, n: Nested, e: NestedEnum, o: NestedObj, t: NestedTrait, a: NestedAnnotation) {
    Outer().Inner2()
}