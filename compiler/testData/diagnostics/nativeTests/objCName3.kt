// FIR_IDENTICAL
// FILE: kotlin.kt
@file:OptIn(ExperimentalObjCName::class)

package kotlin.native
import kotlin.experimental.ExperimentalObjCName

fun interface BaseInterface {
    @ObjCName("close")
    fun close()
}

interface DerivedInterface<S> : BaseInterface {
    override fun close()
}

open class BaseClass {
    @ObjCName("close")
    fun close(){
    }
}

class DerivedClass : BaseClass(), DerivedInterface<Any> {}