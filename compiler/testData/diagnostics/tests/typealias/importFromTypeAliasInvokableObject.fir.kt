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
import simpleObject.<!TYPEALIAS_AS_CALLABLE_QUALIFIER_IN_IMPORT_ERROR("SimpleObject; TestCase")!>SimpleObject<!>.propertyLikeClbl