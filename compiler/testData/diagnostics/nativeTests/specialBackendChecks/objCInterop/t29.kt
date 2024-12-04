// FIR_IDENTICAL
// WITH_PLATFORM_LIBS
import kotlinx.cinterop.*
import platform.darwin.*
import platform.Foundation.*

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
class Zzz : NSAssertionHandler() {
    <!PROPERTY_MUST_BE_VAR!>@OptIn(kotlinx.cinterop.BetaInteropApi::class)
    @ObjCOutlet
    val x: NSObject get() = this<!>
}
