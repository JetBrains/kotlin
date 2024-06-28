// FIR_IDENTICAL
// WITH_PLATFORM_LIBS
import kotlinx.cinterop.*
import platform.darwin.*
import platform.Foundation.*

class Zzz : NSAssertionHandler() {
    <!MUST_BE_UNIT_TYPE!>@OptIn(kotlinx.cinterop.BetaInteropApi::class)
    @ObjCAction
    fun foo() = 42<!>
}
