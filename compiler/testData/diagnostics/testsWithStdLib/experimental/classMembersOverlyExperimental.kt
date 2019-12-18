// !USE_EXPERIMENTAL: kotlin.RequiresOptIn
// FILE: api.kt

package api

@RequiresOptIn(RequiresOptIn.Level.WARNING)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class ExperimentalAPI

@ExperimentalAPI
class C {
    @ExperimentalAPI
    fun function() {}

    @ExperimentalAPI
    val property: String = ""

    @ExperimentalAPI
    class Nested {
        @ExperimentalAPI
        fun nestedFunction() {}
    }
}

// FILE: usage.kt

package usage

import api.*

fun use() {
    val c: <!EXPERIMENTAL_API_USAGE!>C<!> = <!EXPERIMENTAL_API_USAGE!>C<!>()
    c.<!EXPERIMENTAL_API_USAGE!>function<!>()
    c.<!EXPERIMENTAL_API_USAGE!>property<!>
    <!EXPERIMENTAL_API_USAGE!>C<!>.<!EXPERIMENTAL_API_USAGE!>Nested<!>().<!EXPERIMENTAL_API_USAGE!>nestedFunction<!>()
}
