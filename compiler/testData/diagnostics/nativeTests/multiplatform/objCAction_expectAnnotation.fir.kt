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
    fun <!MUST_NOT_HAVE_EXTENSION_RECEIVER!>String<!>.foo() = println(this)

    <!MUST_BE_UNIT_TYPE!>@OptIn(kotlinx.cinterop.BetaInteropApi::class)
    @MyObjcAction
    fun foo() = 42<!>

    <!TWO_OR_LESS_PARAMETERS_ARE_SUPPORTED_HERE!>@OptIn(kotlinx.cinterop.BetaInteropApi::class)
    @MyObjcAction
    fun foo(<!MUST_BE_OBJC_OBJECT_TYPE!>a: String<!>, <!MUST_BE_OBJC_OBJECT_TYPE!>b: String<!>, <!MUST_BE_OBJC_OBJECT_TYPE!>c: String<!>) = println(this)<!>

    @OptIn(kotlinx.cinterop.BetaInteropApi::class)
    @MyObjcAction
    fun action() = println(this)
}

// MODULE: platform()()(common)
// FILE: platform.kt
@OptIn(kotlinx.cinterop.BetaInteropApi::class)
actual typealias MyObjcAction = kotlinx.cinterop.ObjCAction
