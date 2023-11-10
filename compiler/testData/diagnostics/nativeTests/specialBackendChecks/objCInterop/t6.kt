// FIR_IDENTICAL
// WITH_PLATFORM_LIBS
import platform.darwin.*
import platform.Foundation.*

fun foo(s: Array<Any?>) = NSAssertionHandler().handleFailureInFunction("zzz", "zzz", 0L, null, *s)
