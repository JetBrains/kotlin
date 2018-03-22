// !API_VERSION: 1.3
// MODULE: api
// FILE: api.kt

package api

@Experimental(Experimental.Level.WARNING, [Experimental.Impact.COMPILATION])
annotation class ExperimentalCompilationAPI

interface I

@ExperimentalCompilationAPI
class Impl : I

// MODULE: usage(api)
// FILE: usage.kt

package usage

import api.*

open class Base(val i: I)

@UseExperimental(ExperimentalCompilationAPI::class)
class Derived : Base(Impl())

@UseExperimental(ExperimentalCompilationAPI::class)
class Delegated : I by Impl()

@UseExperimental(ExperimentalCompilationAPI::class)
val delegatedProperty by Impl()
operator fun I.getValue(x: Any?, y: Any?) = null
