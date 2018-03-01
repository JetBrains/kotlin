// !API_VERSION: 1.3
// MODULE: api
// FILE: api.kt

package api

@Experimental(Experimental.Level.WARNING, [Experimental.Impact.COMPILATION])
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

// MODULE: usage(api)
// FILE: usage.kt

package usage

import api.*

fun use() {
    val c: <!EXPERIMENTAL_API_USAGE!>C<!> = <!EXPERIMENTAL_API_USAGE!>C<!>()
    c.<!EXPERIMENTAL_API_USAGE!>function<!>()
    c.<!EXPERIMENTAL_API_USAGE!>property<!>
    <!EXPERIMENTAL_API_USAGE!>C<!>.<!EXPERIMENTAL_API_USAGE!>Nested<!>().<!EXPERIMENTAL_API_USAGE!>nestedFunction<!>()
}
