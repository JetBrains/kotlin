// FIR_IDENTICAL
// WITH_PLATFORM_LIBS
import kotlinx.cinterop.*
import platform.darwin.*
import platform.Foundation.*

class Zzz : NSAssertionHandler() {
    <!TWO_OR_LESS_PARAMETERS_ARE_SUPPORTED_HERE!>@OptIn(kotlinx.cinterop.BetaInteropApi::class)
    @ObjCAction
    fun foo(x: NSObject, y: NSObject, z: NSObject) { }<!>
}
