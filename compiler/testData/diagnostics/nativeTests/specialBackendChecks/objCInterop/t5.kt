// RUN_PIPELINE_TILL: FRONTEND
// WITH_PLATFORM_LIBS
import platform.darwin.*
import platform.Foundation.*

fun foo() = NSAssertionHandler().handleFailureInFunction("zzz", "zzz", 0, null, <!STRING_AS_VARIADIC_OBJC_PARAM_IS_AMBIGUOUS!>"qzz"<!>)
