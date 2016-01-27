// !DIAGNOSTICS: -UNUSED_VARIABLE
import kotlin.jvm.JvmStatic

open class B {
    public open val base1 : Int = 1
    public open val base2 : Int = 1
}

class A {
    companion object : B() {
        var p1:Int = 1
            @JvmStatic set(p: Int) {
                p1 = 1
            }

        @JvmStatic val z = 1;

        @JvmStatic override val base1: Int = 0

        override val base2: Int = 0
            @JvmStatic get
    }

    object A : B() {
        var p:Int = 1
            @JvmStatic set(p1: Int) {
                p = 1
            }

        @JvmStatic val z = 1;

        <!OVERRIDE_CANNOT_BE_STATIC!>@JvmStatic override val base1: Int<!> = 0

        @JvmStatic <!NON_FINAL_MEMBER_IN_OBJECT!>open<!> fun f() {}

        override val base2: Int = 0
            <!OVERRIDE_CANNOT_BE_STATIC!>@JvmStatic get<!>
    }

    var p:Int = 1
        <!JVM_STATIC_NOT_IN_OBJECT!>@JvmStatic set(p1: Int)<!> {
            p = 1
        }

    <!JVM_STATIC_NOT_IN_OBJECT!>@JvmStatic val z2<!> = 1;
}