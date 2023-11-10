// FIR_IDENTICAL
// WITH_PLATFORM_LIBS
import platform.darwin.*
import platform.Foundation.*

class Zzz : NSAssertionHandler() {
    fun handleFailureInFunction(functionName: String, file: String, lineNumber: NSInteger /* = Long */, description: String?, vararg args: Any?) { }
}
