// ISSUE: KT-66442
// FILE: 1.kt
package singlePrivateObject

private typealias SinglePrivateObject = Object

object Object {
    fun clbl() {}

    class Shmobject
}

// FILE: 2.kt
import singlePrivateObject.<!INVISIBLE_REFERENCE, TYPEALIAS_AS_CALLABLE_QUALIFIER_IN_IMPORT_ERROR!>SinglePrivateObject<!>.clbl

import singlePrivateObject.<!INVISIBLE_REFERENCE!>SinglePrivateObject<!>.Shmobject
