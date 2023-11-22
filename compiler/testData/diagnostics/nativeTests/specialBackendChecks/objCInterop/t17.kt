import platform.darwin.*
import platform.Foundation.*

class Foo : NSObject(), NSPortDelegateProtocol {
    fun foo() {
        super.handlePortMessage(TODO())
    }
}
