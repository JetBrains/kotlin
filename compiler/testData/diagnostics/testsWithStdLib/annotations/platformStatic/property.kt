// !DIAGNOSTICS: -UNUSED_VARIABLE
import kotlin.platform.platformStatic

open class B {
    public open val base1 : Int = 1
    public open val base2 : Int = 1
}

class A {
    default object : B() {
        var p1:Int = 1
            [platformStatic] set(p: Int) {
                p1 = 1
            }

        [platformStatic] val z = 1;

        [platformStatic] override val base1: Int = 0

        override val base2: Int = 0
            [platformStatic] get
    }

    object A : B() {
        var p:Int = 1
            [platformStatic] set(p1: Int) {
                p = 1
            }

        [platformStatic] val z = 1;

        <!OPEN_CANNOT_BE_STATIC!>[platformStatic] override val base1: Int<!> = 0

        <!OPEN_CANNOT_BE_STATIC!>platformStatic open fun f()<!> {}

        override val base2: Int = 0
            <!OPEN_CANNOT_BE_STATIC!>[platformStatic] get<!>
    }

    var p:Int = 1
        <!PLATFORM_STATIC_NOT_IN_OBJECT!>[platformStatic] set(p1: Int)<!> {
            p = 1
        }

    <!PLATFORM_STATIC_NOT_IN_OBJECT!>[platformStatic] val z2<!> = 1;
}