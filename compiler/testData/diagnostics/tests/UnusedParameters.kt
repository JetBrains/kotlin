import kotlin.reflect.KProperty

class C(a: Int, b: Int, c: Int, d: Int, <!UNUSED_PARAMETER!>e<!>: Int = d, val f: String) {
    init {
        a + a
    }

    val g = b

    init {
        c + c
    }
}

fun f(a: Int, b: Int, <!UNUSED_PARAMETER!>c<!>: Int = b) {
    a + a
}

fun Any.getValue(thisRef: Any?, prop: KProperty<*>): String = ":)"
fun Any.setValue(thisRef: Any?, prop: KProperty<*>, value: String) {
}

fun Any.provideDelegate(thisRef: Any?, prop: KProperty<*>) {
}

fun get(<!UNUSED_PARAMETER!>p<!>: Any) {
}

fun set(<!UNUSED_PARAMETER!>p<!>: Any) {
}

fun foo(s: String) {
    s.<!UNRESOLVED_REFERENCE!>xxx<!> = 1
}