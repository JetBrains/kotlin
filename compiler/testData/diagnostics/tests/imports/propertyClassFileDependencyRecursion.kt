// FILE: propertyClassFileDependencyRecursion.kt
package test

import other.prop

// Note: "prop" is expected to be unresolved and replaced to Any
class PropType: <!UNRESOLVED_REFERENCE!>prop<!>

// Note: this time "prop" should be resolved and type should be inferred for "checkTypeProp"
val checkTypeProp = prop

// FILE: propertyClassFileDependencyRecursionOther.kt
package other

import test.PropType

val prop: PropType? = null
