package a

//NOTE: This test shows a corner case which is not covered fully by current implementation
// All we do now is avoid inserting wrong imports (we do not import anything for declaration from copied block)
// To cover this case properly and insert good imports some other approach should be used (some sophisticated heuristic might do the job)

import a.Outer.*
import a.E.ENTRY

<selection>class Outer {
    inner class Inner {
    }
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

enum class E {
    ENTRY
}

fun f2(i: Inner, n: Nested, e: NestedEnum, o: NestedObj, t: NestedTrait, a: NestedAnnotation) {
}</selection>