// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER
import kotlin.reflect.KProperty

var result = ""

class C

operator fun C.assign(a: String) {
    result = a
}

operator fun Int.assign(a: String) {
}

class ByDelegate {
    val v: Int by Delegate()
}

class Delegate {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Int {
        return 5
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
    }
}

fun test() {
    val c = C()
    c = "hello"
    <!VAL_REASSIGNMENT!>c<!> = <!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>
}

fun test_delegate_priority() {
    val x: ByDelegate = ByDelegate()
    <!VAL_REASSIGNMENT!>x.v<!> = <!TYPE_MISMATCH!>"OK"<!>
}
