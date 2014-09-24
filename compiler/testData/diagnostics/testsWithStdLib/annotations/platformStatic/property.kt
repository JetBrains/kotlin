// !DIAGNOSTICS: -UNUSED_VARIABLE
import kotlin.platform.platformStatic

class A {
    class object {
        var p1:Int = 1
            <!PLATFORM_STATIC_ILLEGAL_USAGE!>[platformStatic] set(p: Int)<!> {
                p1 = 1
            }

        <!PLATFORM_STATIC_ILLEGAL_USAGE!>[platformStatic] val z<!> = 1;
    }

    object A {
        var p:Int = 1
            <!PLATFORM_STATIC_ILLEGAL_USAGE!>[platformStatic] set(p1: Int)<!> {
                p = 1
            }

        <!PLATFORM_STATIC_ILLEGAL_USAGE!>[platformStatic] val z<!> = 1;
    }

    var p:Int = 1
        <!PLATFORM_STATIC_ILLEGAL_USAGE!>[platformStatic] set(p1: Int)<!> {
            p = 1
        }

    <!PLATFORM_STATIC_ILLEGAL_USAGE!>[platformStatic] val z<!> = 1;
}