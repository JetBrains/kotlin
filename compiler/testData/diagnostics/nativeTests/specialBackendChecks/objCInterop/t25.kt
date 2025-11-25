// RUN_PIPELINE_TILL: BACKEND
// WITH_PLATFORM_LIBS
import platform.darwin.*
import platform.Foundation.*

class Zzz : NSAssertionHandler() {
    override fun toString() = "zzz"
}
