// FIR_IDENTICAL
// FILE: kotlin.kt
@file:OptIn(ExperimentalObjCName::class)

package kotlin.native

import kotlin.experimental.ExperimentalObjCName

fun interface BaseInterface {
    @ObjCName("close")
    fun close()
}

interface DerivedInterface : BaseInterface {
    override fun close()
}

fun interface IntersectionInterface {
    @ObjCName("close")
    fun close()
}

open class IntersectionAbstract<T> {
    @ObjCName("close")
    fun close() {
    }
}

open class IntersectionBaseClass<T> : IntersectionAbstract<T>(), IntersectionInterface

class DerivedClass : IntersectionBaseClass<Any>(), DerivedInterface {}