// FIR_IDENTICAL
// FILE: kotlin.kt
@file:OptIn(ExperimentalObjCName::class)

package kotlin.native

import kotlin.experimental.ExperimentalObjCName

fun interface BaseInterface {
    @ObjCName(name = "close")
    fun close()
}

open class BaseClass {
    @ObjCName("close")
    fun close() {
    }
}


class DerivedClass : BaseClass(), BaseInterface {}
