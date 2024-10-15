// RUN_PIPELINE_TILL: SOURCE
// FILE: 1.kt
package objectProperties

typealias ObjectWithProps = A

object A {
    val a = 10
}

// FILE: 2.kt
import objectProperties.ObjectWithProps.a