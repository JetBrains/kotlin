// !API_VERSION: 1.3
// MODULE: api
// FILE: api.kt

package api

@Experimental(Experimental.Level.WARNING, [Experimental.Impact.COMPILATION])
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

// MODULE: usage1(api)
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

// MODULE: usage2(api)
// FILE: usage-use.kt

package usage2

import api.*

@UseExperimental(ExperimentalAPI::class)
fun useAll() {
    val c: C = C()
    c.function()
    c.property
    C.Nested()
    c.Inner()
    c.extension()
}

@UseExperimental(ExperimentalAPI::class)
class Use {
    fun useAll(c: <!EXPERIMENTAL_API_USAGE!>C<!>) {
        c.function()
        c.property
        C.Nested()
        c.Inner()
        c.extension()
    }
}

// MODULE: usage3(api)
// FILE: usage-none.kt

package usage3

import api.*

fun use() {
    val c: <!EXPERIMENTAL_API_USAGE!>C<!> = <!EXPERIMENTAL_API_USAGE!>C<!>()
    c.<!EXPERIMENTAL_API_USAGE!>function<!>()
    c.<!EXPERIMENTAL_API_USAGE!>property<!>
    <!EXPERIMENTAL_API_USAGE!>C<!>.<!EXPERIMENTAL_API_USAGE!>Nested<!>()
    c.<!EXPERIMENTAL_API_USAGE!>Inner<!>()
    c.<!EXPERIMENTAL_API_USAGE!>extension<!>()
}
