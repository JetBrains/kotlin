// ISSUE: KT-25527

import kotlin.reflect.KProperty

fun test_1(delegate: Delegate) {
    var p1 by delegate
    p1 = 10

    var p2 by delegate
    <!UNUSED_CHANGED_VALUE!>p2++<!>

    var p3 by delegate
    ++p3
}

fun test_2() {
    var <!ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE!>p1<!> = 0
    <!UNUSED_VALUE!>p1 =<!> 10

    var <!ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE!>p2<!>: Int
    <!UNUSED_VALUE!>p2 =<!> 10

    var p3 = 1
    <!UNUSED_CHANGED_VALUE!>p3++<!>

    var p4 = 1
    ++p4
}

class Delegate {
    var prop: Int = 0

    operator fun getValue(thisRef: Any?, property: KProperty<*>): Int {
        return prop
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
        prop = value
    }
}
