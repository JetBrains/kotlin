// FIR_IDENTICAL
// OPT_IN: kotlin.RequiresOptIn
// DIAGNOSTICS: -UNUSED_VARIABLE
// FILE: api.kt

package api

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.TYPEALIAS,
        AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.BINARY)
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
    @OptIn(ExperimentalAPI::class)
    {
        function()
        property
        val s: Typealias = ""
    }()
}

@OptIn(ExperimentalAPI::class)
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
    <!OPT_IN_USAGE!>function<!>()
    <!OPT_IN_USAGE!>property<!>
    val s: <!OPT_IN_USAGE!>Typealias<!> = ""
    <!OPT_IN_USAGE!>s<!>.hashCode()
}
