// !API_VERSION: 1.3
// !DIAGNOSTICS: -UNUSED_VARIABLE
// FILE: api.kt

package api

@Experimental(Experimental.Level.WARNING, [Experimental.Impact.COMPILATION])
@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.TYPEALIAS,
        AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.EXPRESSION)
annotation class ExperimentalAPI

@ExperimentalAPI
fun function(): String = ""

@ExperimentalAPI
val property: String = ""

@ExperimentalAPI
typealias Typealias = String

// FILE: usage-propagate.kt

package usage1

import api.*

@ExperimentalAPI
fun useAll() {
    function()
    property
    val s: Typealias = ""
}

@ExperimentalAPI
class Use {
    fun useAll() {
        function()
        property
        val s: Typealias = ""
    }
}

// FILE: usage-use.kt

package usage2

import api.*

fun useAll() {
    @UseExperimental(ExperimentalAPI::class)
    {
        function()
        property
        val s: Typealias = ""
    }()
}

@UseExperimental(ExperimentalAPI::class)
class Use {
    fun useAll() {
        function()
        property
        val s: Typealias = ""
    }
}

// FILE: usage-none.kt

package usage3

import api.*

fun use() {
    <!EXPERIMENTAL_API_USAGE!>function<!>()
    <!EXPERIMENTAL_API_USAGE!>property<!>
    val s: <!EXPERIMENTAL_API_USAGE!>Typealias<!> = ""
    s.hashCode()
}
