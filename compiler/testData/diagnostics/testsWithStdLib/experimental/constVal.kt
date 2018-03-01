// !API_VERSION: 1.3
// MODULE: api
// FILE: api.kt

package api

@Experimental(Experimental.Level.WARNING, [Experimental.Impact.COMPILATION])
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
annotation class ExperimentalAPI

@ExperimentalAPI
const val MEANING = 42

annotation class Anno(val value: Int)

// MODULE: usage1(api)
// FILE: usage-propagate.kt

package usage1

import api.*

@ExperimentalAPI
@Anno(MEANING)
fun usage() {}

// MODULE: usage2(api)
// FILE: usage-use.kt

@file:UseExperimental(ExperimentalAPI::class)
package usage2

import api.*

@Anno(<!EXPERIMENTAL_API_USAGE!>MEANING<!>)
fun usage() {}

// MODULE: usage3(api)
// FILE: usage-none.kt

package usage3

import api.*

@Anno(<!EXPERIMENTAL_API_USAGE!>MEANING<!>)
fun usage() {}
