// FILE: 1.kt
package objectInClass

class Outer1 {
    class Nested {
        object Object {
            fun clbl1() {}
        }
    }
}

typealias ObjectInNestedClass = Outer1.Nested.Object

// FILE: 2.kt
import objectInClass.<!TYPEALIAS_AS_CALLABLE_QUALIFIER_IN_IMPORT_ERROR("ObjectInNestedClass; Object")!>ObjectInNestedClass<!>.clbl1