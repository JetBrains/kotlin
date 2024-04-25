// LANGUAGE: +MultiPlatformProjects
// DIAGNOSTICS: -UNUSED_PARAMETER -UNRESOLVED_REFERENCE
// WITH_STDLIB
// WITH_PLATFORM_LIBS

// MODULE: common
// FILE: common.kt

import kotlinx.cinterop.*
import platform.darwin.*
import platform.Foundation.*

@Target(AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.SOURCE)
expect annotation class <!NO_ACTUAL_FOR_EXPECT!>MyOverrideInit<!>

class DoesNotOverride : NSAssertionHandler {
    @OptIn(kotlinx.cinterop.BetaInteropApi::class)
    @<!NO_CONSTRUCTOR!>MyOverrideInit<!>
    constructor(x: Int) { }
}

class OverridesOverriden : NSString {
    @OptIn(kotlinx.cinterop.BetaInteropApi::class)
    @<!NO_CONSTRUCTOR!>MyOverrideInit<!>
    constructor(coder: NSCoder) { }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun initWithCoder(coder: NSCoder): String? = "x"
}

// MODULE: platform()()(common)
// FILE: platform.kt
@OptIn(kotlinx.cinterop.BetaInteropApi::class)
actual typealias MyOverrideInit = kotlinx.cinterop.ObjCObjectBase.OverrideInit
