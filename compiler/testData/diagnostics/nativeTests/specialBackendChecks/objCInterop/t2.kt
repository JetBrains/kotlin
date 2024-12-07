// FIR_IDENTICAL
// WITH_PLATFORM_LIBS
import platform.darwin.*
import platform.Foundation.*

// KT-42161, KT-63699: Must be not `NOTHING_TO_OVERRIDE` diagnostic, but `overriding variadic Objective-C methods is not supported`
class Zzz : NSAssertionHandler() {
    <!NOTHING_TO_OVERRIDE!>override<!> fun handleFailureInFunction(functionName: String, file: String, lineNumber: NSInteger /* = Long */, description: String?, vararg args: Any?) { }
}
