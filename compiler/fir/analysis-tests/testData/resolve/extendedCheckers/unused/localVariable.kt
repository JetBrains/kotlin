import kotlin.reflect.KProperty

class Outer {
    fun foo() {
        class Local {
            fun bar() {
                <!UNUSED_VARIABLE{LT}!>val <!UNUSED_VARIABLE{PSI}!>x<!> = y<!>
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
    <!UNUSED_VARIABLE{LT}!>val <!UNUSED_VARIABLE{PSI}!>d<!>: Int by Delegate<!>
    <!UNUSED_VARIABLE{LT}!>val <!UNUSED_VARIABLE{PSI}!>a<!>: Int<!>
    <!UNUSED_VARIABLE{LT}!>val <!UNUSED_VARIABLE{PSI}!>b<!> = 1<!>
    val c = 2

    <!UNUSED_VARIABLE{LT}!>@Anno
    val <!UNUSED_VARIABLE{PSI}!>e<!>: Int<!>

    foo(c)
}

object Delegate {
    operator fun getValue(instance: Any?, property: KProperty<*>) = 1
    operator fun setValue(instance: Any?, property: KProperty<*>, value: String) {}
}

@Target(AnnotationTarget.LOCAL_VARIABLE)
annotation class Anno
