// !USE_EXPERIMENTAL: kotlin.Experimental
// FILE: api.kt

package api

@Experimental(Experimental.Level.WARNING)
annotation class ExperimentalAPI

@ExperimentalAPI
@Experimental(Experimental.Level.WARNING)
annotation class VeryExperimentalAPI

@ExperimentalAPI
@VeryExperimentalAPI
fun f() {}

@ExperimentalAPI
fun g() {}

// FILE: usage.kt

@file:UseExperimental(ExperimentalAPI::class, VeryExperimentalAPI::class)
package usage

import api.*

fun usage() {
    f()
    g()
}
