import kotlin.reflect.KProperty

class Outer {
    fun foo() {
        class Local {
            fun bar() {
                val <!UNUSED_VARIABLE!>x<!> = y
            }
        }
    }

    val y = ""
}

fun f() {
    val a = 1

    fun g(): Int {
        return a
    }
}


fun foo(v: Int) {
    val <!UNUSED_VARIABLE!>d<!>: Int by Delegate
    val <!UNUSED_VARIABLE!>a<!>: Int
    val <!UNUSED_VARIABLE!>b<!> = 1
    val c = 2

    @Anno
    val <!UNUSED_VARIABLE!>e<!>: Int

    foo(c)
}

object Delegate {
    operator fun getValue(instance: Any?, property: KProperty<*>) = 1
    operator fun setValue(instance: Any?, property: KProperty<*>, value: String) {}
}

@Target(AnnotationTarget.LOCAL_VARIABLE)
annotation class Anno
