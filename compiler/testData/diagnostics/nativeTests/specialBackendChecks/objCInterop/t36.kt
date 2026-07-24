// RUN_PIPELINE_TILL: BACKEND
// WITH_PLATFORM_LIBS
// KT-77446
import kotlinx.cinterop.*
import platform.darwin.*

// This test expects "a local Kotlin Obj-C class 'Foo' with an @kotlinx.cinterop.ObjCObjectBase.OverrideInit constructor cannot capture values from the enclosing scope. Captured: 'x'" error.
fun outer() {
    val x = 123
    class Foo @OptIn(kotlinx.cinterop.BetaInteropApi::class) @OverrideInit constructor() : NSObject() {
        val y = x
    }
}
