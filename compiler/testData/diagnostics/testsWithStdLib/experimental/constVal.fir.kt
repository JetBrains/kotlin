// !USE_EXPERIMENTAL: kotlin.Experimental
// FILE: api.kt

package api

@Experimental(Experimental.Level.WARNING)
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
annotation class ExperimentalAPI

@ExperimentalAPI
const val MEANING = 42

annotation class Anno(val value: Int)

// FILE: usage-propagate.kt

package usage1

import api.*

@ExperimentalAPI
@Anno(MEANING)
fun usage() {}

// FILE: usage-use.kt

@file:UseExperimental(ExperimentalAPI::class)
package usage2

import api.*

// TODO: there should be no warning here
@Anno(MEANING)
fun usage() {}

// FILE: usage-none.kt

package usage3

import api.*

@Anno(MEANING)
fun usage() {}
