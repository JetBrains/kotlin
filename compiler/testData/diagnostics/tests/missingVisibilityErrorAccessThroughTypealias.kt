// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-66442
// FILE: 1.kt
package singlePrivateObject

private typealias SinglePrivateObject = Object

object Object {
    fun clbl() {}

    class Shmobject
}

// FILE: 2.kt
import singlePrivateObject.<!INVISIBLE_REFERENCE!>SinglePrivateObject<!>.clbl

import singlePrivateObject.<!INVISIBLE_REFERENCE!>SinglePrivateObject<!>.Shmobject
