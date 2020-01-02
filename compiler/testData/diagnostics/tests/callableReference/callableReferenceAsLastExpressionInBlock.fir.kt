// !DIAGNOSTICS: -UNUSED_VARIABLE
// !CHECK_TYPE

import kotlin.reflect.KFunction0

fun test() {
    val a = if (true) {
        val x = 1
        "".length
        <!UNRESOLVED_REFERENCE!>::foo<!>
    } else {
        <!UNRESOLVED_REFERENCE!>::foo<!>
    }
    a checkType {  <!UNRESOLVED_REFERENCE!>_<!><KFunction0<Int>>() }
}

fun foo(): Int = 0