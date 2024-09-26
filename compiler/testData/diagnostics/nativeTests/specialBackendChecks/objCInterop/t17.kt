// FIR_IDENTICAL
// WITH_PLATFORM_LIBS
import kotlinx.cinterop.ExperimentalForeignApi
import platform.darwin.*
import platform.Foundation.*

class Foo : NSObject(), NSPortDelegateProtocol {
    @OptIn(ExperimentalForeignApi::class)
    // ^^^ Added opt-in because of the difference in NSPortDelegateProtocol.handlePortMessage() signature
    //     on different platforms. On some targets, the signature uses experimental C-interop API.
    //
    //     Examples:
    //     macos_arm64:         public open fun handlePortMessage(message: platform.Foundation.NSPortMessage)
    //     ios_simulator_arm64: public open fun handlePortMessage(message: objcnames.classes.NSPortMessage)
    fun foo() {
        super.handlePortMessage(TODO())
    }
}
