// FIR_IDENTICAL
// WITH_PLATFORM_LIBS
import kotlinx.cinterop.*
import platform.darwin.*
import platform.Foundation.*

class Zzz : NSString {
    @OptIn(kotlinx.cinterop.BetaInteropApi::class)
    @OverrideInit
    constructor(coder: NSCoder) { }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun initWithCoder(coder: NSCoder): String? = "zzz"
}
