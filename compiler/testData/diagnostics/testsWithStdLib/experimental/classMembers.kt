// FIR_IDENTICAL
// OPT_IN: kotlin.RequiresOptIn
// FILE: api.kt

package api

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class ExperimentalAPI

@ExperimentalAPI
class C {
    fun function(): String = ""
    val property: String = ""
    class Nested
    inner class Inner
}

@ExperimentalAPI
fun C.extension() {}

// FILE: usage-propagate.kt

package usage1

import api.*

@ExperimentalAPI
fun useAll() {
    val c: C = C()
    c.function()
    c.property
    C.Nested()
    c.Inner()
    c.extension()
}

@ExperimentalAPI
class Use {
    fun useAll(c: C) {
        c.function()
        c.property
        C.Nested()
        c.Inner()
        c.extension()
    }
}

// FILE: usage-use.kt

package usage2

import api.*

@OptIn(ExperimentalAPI::class)
fun useAll() {
    val c: C = C()
    c.function()
    c.property
    C.Nested()
    c.Inner()
    c.extension()
}

@OptIn(ExperimentalAPI::class)
class Use {
    fun useAll(c: C) {
        c.function()
        c.property
        C.Nested()
        c.Inner()
        c.extension()
    }
}

// FILE: usage-none.kt

package usage3

import api.*

fun use() {
    val c: <!OPT_IN_USAGE!>C<!> = <!OPT_IN_USAGE!>C<!>()
    <!OPT_IN_USAGE!>c<!>.<!OPT_IN_USAGE!>function<!>()
    <!OPT_IN_USAGE!>c<!>.<!OPT_IN_USAGE!>property<!>
    <!OPT_IN_USAGE!>C<!>.<!OPT_IN_USAGE!>Nested<!>()
    <!OPT_IN_USAGE!>c<!>.<!OPT_IN_USAGE!>Inner<!>()
    <!OPT_IN_USAGE!>c<!>.<!OPT_IN_USAGE!>extension<!>()
}
