// LL_FIR_DIVERGENCE
// Checkers are run with Common session in Analysis API, so they can't see actualized declarations
// LL_FIR_DIVERGENCE
// LANGUAGE: +MultiPlatformProjects
// DIAGNOSTICS: -UNUSED_PARAMETER
// WITH_STDLIB
// WITH_PLATFORM_LIBS

// MODULE: common
// FILE: common.kt

import platform.Foundation.*

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
@kotlinx.cinterop.BetaInteropApi
expect annotation class MyObjcAction()

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
class TestClass : NSAssertionHandler() {
    @OptIn(kotlinx.cinterop.BetaInteropApi::class)
    @MyObjcAction
    fun String.foo() = println(this)

    @OptIn(kotlinx.cinterop.BetaInteropApi::class)
    @MyObjcAction
    fun foo() = 42

    @OptIn(kotlinx.cinterop.BetaInteropApi::class)
    @MyObjcAction
    fun foo(a: String, b: String, c: String) = println(this)

    @OptIn(kotlinx.cinterop.BetaInteropApi::class)
    @MyObjcAction
    fun action() = println(this)
}

// MODULE: platform()()(common)
// FILE: platform.kt
@OptIn(kotlinx.cinterop.BetaInteropApi::class)
actual typealias MyObjcAction = kotlinx.cinterop.ObjCAction
