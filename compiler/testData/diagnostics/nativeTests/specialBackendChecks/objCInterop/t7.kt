// RUN_PIPELINE_TILL: FRONTEND
// WITH_PLATFORM_LIBS
import platform.darwin.*
import platform.Foundation.*

fun foo(s: Array<Any?>) = NSLog("zzz", <!VARIADIC_C_SPREAD_IS_SUPPORTED_ONLY_FOR_ARRAYOF!>*s<!>)
fun bar(s: Array<Any?>) = NSLog("zzz", *arrayOf(*arrayOf(<!VARIADIC_C_SPREAD_IS_SUPPORTED_ONLY_FOR_ARRAYOF!>*s<!>)))
