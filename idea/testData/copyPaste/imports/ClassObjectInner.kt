package a

import a.Outer.Companion.Nested
import a.Outer.Companion.NestedEnum
import a.Outer.Companion.NestedObj
import a.Outer.Companion.NestedTrait
import a.Outer.Companion.NestedAnnotation

class Outer {
    companion object {
        class Nested {
        }
        enum class NestedEnum {
        }
        object NestedObj {
        }
        interface NestedTrait {
        }
        annotation class NestedAnnotation
    }
}

<selection>fun f(n: Nested, e: NestedEnum, o: NestedObj, t: NestedTrait, a: NestedAnnotation) {
}</selection>