// FIR_IDENTICAL
// FILE: kotlin.kt
@file:OptIn(ExperimentalObjCName::class)

package kotlin.native
import kotlin.experimental.ExperimentalObjCName

fun interface BaseInterface {
    @ObjCName("close")
    fun close()
}

interface MiddleInterface<S> : BaseInterface {
    override fun close()
}

interface DerivedInterface<S> : MiddleInterface<S> {
    override fun close()
}

open class BaseClass {
    @ObjCName("close")
    fun close() {
    }
}

class DerivedClass : BaseClass(), DerivedInterface<Any> {}