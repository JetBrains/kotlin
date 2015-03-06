// !DIAGNOSTICS: -UNUSED_VARIABLE
import kotlin.platform.platformStatic

class A {
    default object {
        platformStatic fun a1() {

        }
    }

    object A {
        platformStatic fun a2() {

        }
    }

    fun test() {
        val s = object {
            <!PLATFORM_STATIC_NOT_IN_OBJECT!>platformStatic fun a3()<!> {

            }
        }
    }

    <!PLATFORM_STATIC_NOT_IN_OBJECT!>platformStatic fun a4()<!> {

    }
}

trait B {
    default object {
        <!PLATFORM_STATIC_NOT_IN_OBJECT!>platformStatic fun a1()<!> {

        }
    }

    object A {
        platformStatic fun a2() {

        }
    }

    fun test() {
        val s = object {
            <!PLATFORM_STATIC_NOT_IN_OBJECT!>platformStatic fun a3()<!> {

            }
        }
    }

    <!PLATFORM_STATIC_NOT_IN_OBJECT!>platformStatic fun a4()<!> {

    }
}