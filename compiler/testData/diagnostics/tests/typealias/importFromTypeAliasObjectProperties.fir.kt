// FILE: 1.kt
package objectProperties

typealias ObjectWithProps = A

object A {
    val a = 10
}

// FILE: 2.kt
import objectProperties.<!TYPEALIAS_AS_CALLABLE_QUALIFIER_IN_IMPORT_ERROR("ObjectWithProps; A")!>ObjectWithProps<!>.a
