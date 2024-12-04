// FIR_IDENTICAL
// WITH_PLATFORM_LIBS
import kotlinx.cinterop.*
import platform.darwin.*
import platform.Foundation.*

class Zzz : NSAssertionHandler {
    <!CONSTRUCTOR_DOES_NOT_OVERRIDE_ANY_SUPER_CONSTRUCTOR!>@OptIn(kotlinx.cinterop.BetaInteropApi::class)
    @OverrideInit
    constructor(x: Int) { }<!>
}
