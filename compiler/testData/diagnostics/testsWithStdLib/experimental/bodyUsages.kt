// !USE_EXPERIMENTAL: kotlin.Experimental
// FILE: api.kt

package api

@Experimental(Experimental.Level.WARNING)
annotation class ExperimentalAPI

interface I

@ExperimentalAPI
class Impl : I

// FILE: usage.kt

package usage

import api.*

open class Base(val i: I)

@UseExperimental(ExperimentalAPI::class)
class Derived : Base(Impl())

@UseExperimental(ExperimentalAPI::class)
class Delegated : I by Impl()

@UseExperimental(ExperimentalAPI::class)
val delegatedProperty by Impl()
operator fun I.getValue(x: Any?, y: Any?) = null
