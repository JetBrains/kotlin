import kotlin.platform.platformStatic

platformStatic
class <error descr="[PLATFORM_STATIC_ILLEGAL_USAGE] This declaration does not support 'platformStatic'">A</error> {
    platformStatic
    default <error descr="[PLATFORM_STATIC_ILLEGAL_USAGE] This declaration does not support 'platformStatic'">object</error> {
        platformStatic fun a1() {

        }
    }

    platformStatic
    <error descr="[PLATFORM_STATIC_ILLEGAL_USAGE] This declaration does not support 'platformStatic'">object A</error> {
        platformStatic fun a2() {

        }
    }

    fun test() {
        val <warning descr="[UNUSED_VARIABLE] Variable 's' is never used">s</warning> = object {
            <error descr="[PLATFORM_STATIC_NOT_IN_OBJECT] Only functions in named objects and default objects of classes can be annotated with 'platformStatic'">platformStatic fun a3()</error> {

            }
        }
    }

    <error descr="[PLATFORM_STATIC_NOT_IN_OBJECT] Only functions in named objects and default objects of classes can be annotated with 'platformStatic'">platformStatic fun a4()</error> {

    }
}

platformStatic
trait <error descr="[PLATFORM_STATIC_ILLEGAL_USAGE] This declaration does not support 'platformStatic'">B</error> {
    default object {
        <error descr="[PLATFORM_STATIC_NOT_IN_OBJECT] Only functions in named objects and default objects of classes can be annotated with 'platformStatic'">platformStatic fun a1()</error> {

        }
    }

    object A {
        platformStatic fun a2() {

        }
    }

    fun test() {
        val <warning descr="[UNUSED_VARIABLE] Variable 's' is never used">s</warning> = object {
            <error descr="[PLATFORM_STATIC_NOT_IN_OBJECT] Only functions in named objects and default objects of classes can be annotated with 'platformStatic'">platformStatic fun a3()</error> {

            }
        }
    }

    <error descr="[PLATFORM_STATIC_NOT_IN_OBJECT] Only functions in named objects and default objects of classes can be annotated with 'platformStatic'">platformStatic fun a4()</error> {

    }
}