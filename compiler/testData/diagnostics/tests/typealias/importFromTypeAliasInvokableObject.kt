// FILE: 1.kt
package simpleObject

typealias SimpleObject = TestCase

object InvokableObject {
    operator fun invoke() {}
}

object TestCase {
    val propertyLikeClbl = InvokableObject
}

// FILE: 2.kt
import simpleObject.SimpleObject.propertyLikeClbl