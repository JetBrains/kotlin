// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE
// !LANGUAGE: +CalledInPlaceEffect

import kotlin.internal.*

fun <T> inPlace(@CalledInPlace block: () -> T): T = block()

fun reassignmentAndNoInitializaiton() {
    val x: Int
    inPlace { <!VAL_REASSIGNMENT!>x<!> = 42 }
    <!UNINITIALIZED_VARIABLE!>x<!>.inc()
}