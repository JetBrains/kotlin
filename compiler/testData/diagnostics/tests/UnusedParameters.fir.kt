// !DIAGNOSTICS: +UNUSED_PARAMETER
import kotlin.reflect.KProperty

class C(a: Int, b: Int, c: Int, d: Int, e: Int = d, val f: String) {
    init {
        a + a
    }

    val g = b

    init {
        c + c
    }
}

fun f(a: Int, b: Int, c: Int = b) {
    a + a
}

fun Any.getValue(thisRef: Any?, prop: KProperty<*>): String = ":)"
fun Any.setValue(thisRef: Any?, prop: KProperty<*>, value: String) {
}

fun Any.provideDelegate(thisRef: Any?, prop: KProperty<*>) {
}

operator fun Int.getValue(thisRef: Any?, prop: KProperty<*>): String = ":)"

operator fun Int.setValue(thisRef: Any?, prop: KProperty<*>, value: String) {
}

operator fun Int.provideDelegate(thisRef: Any?, prop: KProperty<*>) {
}


fun get(p: Any) {
}

fun set(p: Any) {
}

fun foo(s: String) {
    s.<!UNRESOLVED_REFERENCE!>xxx<!> = 1
}
