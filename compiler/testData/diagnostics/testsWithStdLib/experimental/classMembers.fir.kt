// !USE_EXPERIMENTAL: kotlin.RequiresOptIn
// FILE: api.kt

package api

@RequiresOptIn(RequiresOptIn.Level.WARNING)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
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
    val c: C = C()
    c.function()
    c.property
    C.Nested()
    c.Inner()
    c.extension()
}
