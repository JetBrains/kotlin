// !DIAGNOSTICS: -UNUSED_VARIABLE
// !CHECK_TYPE

import kotlin.reflect.KFunction0

fun test() {
    val a = if (true) {
        val x = 1
        "".length
        ::foo
    } else {
        ::foo
    }
    a checkType {  _<KFunction0<Int>>() }
}

fun foo(): Int = 0