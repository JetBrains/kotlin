// RUN_PIPELINE_TILL: FRONTEND
// WITH_PLATFORM_LIBS
import platform.darwin.*
import platform.Foundation.*

fun foo(s: Array<Any?>) = NSAssertionHandler().handleFailureInFunction("zzz", "zzz", 0, null, <!VARIADIC_OBJC_SPREAD_IS_SUPPORTED_ONLY_FOR_ARRAYOF!>*s<!>)
fun bar(s: Array<Any?>) = NSAssertionHandler().handleFailureInFunction("zzz", "zzz", 0, null, *arrayOf(*arrayOf(<!VARIADIC_OBJC_SPREAD_IS_SUPPORTED_ONLY_FOR_ARRAYOF!>*s<!>)))
