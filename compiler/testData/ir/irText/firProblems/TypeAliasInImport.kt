// FIR_IDENTICAL
// ISSUE: KT-65771
// SKIP_KLIB_TEST
// REASON: KT-68988

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
