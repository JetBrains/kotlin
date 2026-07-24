// RUN_PIPELINE_TILL: BACKEND
// WITH_PLATFORM_LIBS
// KT-77446
import kotlinx.cinterop.*
import platform.darwin.*

// This test expects "a local Kotlin Obj-C class 'Foo' with an @kotlinx.cinterop.ObjCObjectBase.OverrideInit constructor cannot capture values from the enclosing scope. Captured: 'T'" error.
fun <T> outer() {
    class Foo @OptIn(kotlinx.cinterop.BetaInteropApi::class) @OverrideInit constructor() : NSObject() {
        val v: T? = null
    }
}
