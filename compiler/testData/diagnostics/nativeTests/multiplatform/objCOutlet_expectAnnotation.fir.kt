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
    <!PROPERTY_MUST_BE_VAR!>@OptIn(kotlinx.cinterop.BetaInteropApi::class)
    @MyObjcOutlet
    val x: NSObject get() = this<!>

    @OptIn(kotlinx.cinterop.BetaInteropApi::class)
    @MyObjcOutlet
    var y: <!MUST_BE_OBJC_OBJECT_TYPE!>String<!>
        get() = "y"
        set(value: String) { }

    @OptIn(kotlinx.cinterop.BetaInteropApi::class)
    @MyObjcOutlet
    var <!MUST_NOT_HAVE_EXTENSION_RECEIVER!>NSObject<!>.x: NSObject
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
