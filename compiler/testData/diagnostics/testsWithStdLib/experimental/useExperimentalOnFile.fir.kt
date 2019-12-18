// !USE_EXPERIMENTAL: kotlin.RequiresOptIn
// FILE: api.kt

package api

@RequiresOptIn(RequiresOptIn.Level.WARNING)
@Target(AnnotationTarget.FUNCTION)
annotation class ExperimentalAPI1

@RequiresOptIn(RequiresOptIn.Level.WARNING)
@Target(AnnotationTarget.FUNCTION)
annotation class ExperimentalAPI2

@ExperimentalAPI1
fun compilation() {}

@ExperimentalAPI2
fun runtime() {}

// FILE: usage.kt

@file:OptIn(ExperimentalAPI1::class)
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
