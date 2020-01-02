// !USE_EXPERIMENTAL: kotlin.Experimental
// FILE: api.kt

package api

@Experimental(Experimental.Level.WARNING)
@Target(AnnotationTarget.FUNCTION)
annotation class ExperimentalAPI1

@Experimental(Experimental.Level.WARNING)
@Target(AnnotationTarget.FUNCTION)
annotation class ExperimentalAPI2

@ExperimentalAPI1
fun compilation() {}

@ExperimentalAPI2
fun runtime() {}

// FILE: usage.kt

@file:UseExperimental(ExperimentalAPI1::class)
package usage

import api.*

fun use() {
    compilation()
    runtime()
}

class Use {
    fun use() {
        compilation()
        runtime()
    }
}
