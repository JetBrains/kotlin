// !DIAGNOSTICS: -UNUSED_VARIABLE
import kotlin.jvm.jvmStatic

open class B {
    public open val base1 : Int = 1
    public open val base2 : Int = 1
}

class A {
    companion object : B() {
        var p1:Int = 1
            @jvmStatic set(p: Int) {
                p1 = 1
            }

        @jvmStatic val z = 1;

        @jvmStatic override val base1: Int = 0

        override val base2: Int = 0
            @jvmStatic get
    }

    object A : B() {
        var p:Int = 1
            @jvmStatic set(p1: Int) {
                p = 1
            }

        @jvmStatic val z = 1;

        <!OVERRIDE_CANNOT_BE_STATIC!>@jvmStatic override val base1: Int<!> = 0

        jvmStatic open fun f() {}

        override val base2: Int = 0
            <!OVERRIDE_CANNOT_BE_STATIC!>@jvmStatic get<!>
    }

    var p:Int = 1
        <!JVM_STATIC_NOT_IN_OBJECT!>@jvmStatic set(p1: Int)<!> {
            p = 1
        }

    <!JVM_STATIC_NOT_IN_OBJECT!>@jvmStatic val z2<!> = 1;
}