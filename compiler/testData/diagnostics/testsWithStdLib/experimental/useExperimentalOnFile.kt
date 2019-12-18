// !USE_EXPERIMENTAL: kotlin.RequiresOptIn
// FILE: api.kt

package api

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@Target(AnnotationTarget.FUNCTION)
annotation class ExperimentalAPI1

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
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
    <!EXPERIMENTAL_API_USAGE!>runtime<!>()
}

class Use {
    fun use() {
        compilation()
        <!EXPERIMENTAL_API_USAGE!>runtime<!>()
    }
}
