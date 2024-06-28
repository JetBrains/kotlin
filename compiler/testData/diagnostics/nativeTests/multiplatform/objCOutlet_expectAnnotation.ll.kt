// LL_FIR_DIVERGENCE
// Checkers are run with Common session in Analysis API, so they can't see actualized declarations
// LL_FIR_DIVERGENCE
// LANGUAGE: +MultiPlatformProjects
// DIAGNOSTICS: -UNUSED_PARAMETER
// WITH_STDLIB
// WITH_PLATFORM_LIBS

// MODULE: common
// FILE: common.kt

import kotlinx.cinterop.*
import platform.darwin.*
import platform.Foundation.*

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
@kotlinx.cinterop.BetaInteropApi
expect annotation class MyObjcOutlet()

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
class VarProperty : NSAssertionHandler() {
    @OptIn(kotlinx.cinterop.BetaInteropApi::class)
    @MyObjcOutlet
    val x: NSObject get() = this

    @OptIn(kotlinx.cinterop.BetaInteropApi::class)
    @MyObjcOutlet
    var y: String
        get() = "y"
        set(value: String) { }

    @OptIn(kotlinx.cinterop.BetaInteropApi::class)
    @MyObjcOutlet
    var NSObject.x: NSObject
        get() = this
        set(value: NSObject) { }

    @OptIn(kotlinx.cinterop.BetaInteropApi::class)
    @MyObjcOutlet
    var outlet: NSObject = NSObject()
}

// MODULE: platform()()(common)
// FILE: platform.kt
@OptIn(kotlinx.cinterop.BetaInteropApi::class)
actual typealias MyObjcOutlet = kotlinx.cinterop.ObjCOutlet
