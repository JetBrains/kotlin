// FILE: 1.kt
package simpleObject

typealias SimpleObject = TestCase

fun foo() {}

object TestCase {
    val functionReference = ::foo
}

// FILE: 2.kt
import simpleObject.SimpleObject.functionReference