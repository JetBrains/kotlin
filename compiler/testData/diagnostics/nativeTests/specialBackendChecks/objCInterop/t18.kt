// FIR_IDENTICAL
// WITH_PLATFORM_LIBS
import platform.darwin.*

class Foo : NSObject() {
    companion object : NSObjectMeta() {
        fun bar() {
            super.hash()
        }
    }
}
