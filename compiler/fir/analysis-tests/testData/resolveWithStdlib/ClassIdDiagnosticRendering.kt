// RUN_PIPELINE_TILL: FRONTEND
// WITH_EXTRA_CHECKERS

package a.b.c

import <!PLATFORM_CLASS_MAPPED_TO_KOTLIN("kotlin.jvm.functions.Function0")!>kotlin.jvm.functions.Function0<!>

@RequiresOptIn
annotation class Marker

@Marker
class Some

fun main() {
    <!OPT_IN_USAGE_ERROR("a.b.c.Marker; This declaration requires opt-in to be used. The usage must be annotated with '@a.b.c.Marker' or '@OptIn(a.b.c.Marker::class)'")!>Some<!>()
}

annotation class NotOptIn

@SubclassOptInRequired(<!SUBCLASS_OPT_IN_ARGUMENT_IS_NOT_MARKER!>NotOptIn::class<!>)
open class IncorrectSubclassOptInArgumentMarker

internal fun interface StableInterface {
    @Marker
    fun experimentalMethod()
}

fun testOverrides() {
    object : StableInterface {
        override fun <!OPT_IN_OVERRIDE_ERROR("a.b.c.Marker; Base declaration of supertype 'StableInterface' requires opt-in to be overridden. The overriding declaration must be annotated with '@a.b.c.Marker' or '@OptIn(a.b.c.Marker::class)'")!>experimentalMethod<!>() {}
    }
}

<!NOT_A_MULTIPLATFORM_COMPILATION!>actual<!> typealias TypealiasToKotlinPkg = <!ACTUAL_TYPEALIAS_TO_SPECIAL_ANNOTATION("kotlin.Deprecated")!>kotlin.Deprecated<!>
