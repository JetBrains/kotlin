// FIR_IDENTICAL
// LANGUAGE: -ProhibitTypealiasAsCallableQualifierInImport
// ISSUE: KT-65771

// FILE: b.kt
package b

object Obj {
    fun method() {}
    val prop = 1
}

typealias ObjTA = Obj

// FILE: a.kt
package a

import b.ObjTA.method
import b.ObjTA.prop
import b.Obj.method as methodO
import b.Obj.prop as propO

fun test() {
    method()
    prop
    methodO()
    propO
}
