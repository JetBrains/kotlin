package a

import a.Outer.Default.Nested
import a.Outer.Default.NestedEnum
import a.Outer.Default.NestedObj
import a.Outer.Default.NestedTrait
import a.Outer.Default.NestedAnnotation

class Outer {
    class object {
        class Nested {
        }
        enum class NestedEnum {
        }
        object NestedObj {
        }
        trait NestedTrait {
        }
        annotation class NestedAnnotation
    }
}

<selection>fun f(n: Nested, e: NestedEnum, o: NestedObj, t: NestedTrait, a: NestedAnnotation) {
}</selection>