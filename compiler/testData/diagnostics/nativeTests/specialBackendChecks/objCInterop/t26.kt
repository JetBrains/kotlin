// FIR_IDENTICAL
// WITH_PLATFORM_LIBS
import kotlinx.cinterop.*
import platform.darwin.*
import platform.Foundation.*

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
class Zzz : NSAssertionHandler() {
    @OptIn(kotlinx.cinterop.BetaInteropApi::class)
    @ObjCAction
    fun <!MUST_NOT_HAVE_EXTENSION_RECEIVER!>String<!>.foo() = println(this)
}
