// FIR_IDENTICAL
// WITH_PLATFORM_LIBS
import kotlinx.cinterop.*
import platform.darwin.*
import platform.Foundation.*

class Zzz : NSAssertionHandler() {
    @OptIn(kotlinx.cinterop.BetaInteropApi::class)
    @ObjCOutlet
    var <!MUST_NOT_HAVE_EXTENSION_RECEIVER!>NSObject<!>.x: NSObject
        get() = this
        set(value: NSObject) { }
}
